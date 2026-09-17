package io.novafoundation.nova.feature_wallet_connect_impl.presentation.service

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.SignSource
import io.novafoundation.nova.common.navigation.awaitResponse
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignRequester
import io.novafoundation.nova.feature_external_sign_api.model.awaitConfirmation
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignWallet
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.SigningDappMetadata
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.WalletConnectError
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.failed
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.respondSessionRequest
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.ApproveSessionRequester
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsEvent
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.sessionEventsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

private const val UNKNOWN_CHAIN = "unknown"

internal class RealWalletConnectService(
    parentScope: CoroutineScope,
    private val interactor: WalletConnectSessionInteractor,
    private val dAppSignRequester: ExternalSignRequester,
    private val approveSessionRequester: ApproveSessionRequester,
    private val analyticsService: AnalyticsService,
) : WalletConnectService,
    CoroutineScope by parentScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(parentScope) {

    private val events = WalletKit.sessionEventsFlow(scope = this)

    override val onPairErrorLiveData: MutableLiveData<Event<Throwable>> = MutableLiveData()

    init {
        events.onEach {
            when (it) {
                is WalletConnectSessionsEvent.SessionProposal -> handleSessionProposal(it.proposal)
                is WalletConnectSessionsEvent.SessionRequest -> handleSessionRequest(it.request)
                is WalletConnectSessionsEvent.SessionSettlement -> handleSessionSettlement(it.settlement)
                is WalletConnectSessionsEvent.SessionDeleted -> handleSessionDelete(it.delete)
            }
        }
            .inBackground()
            .launchIn(this)
    }

    override fun connect() {
        CoreClient.Relay.connect { error: Core.Model.Error ->
            Log.d(LOG_TAG, "Failed to connect to Wallet Connect: ", error.throwable)
        }
    }

    override fun disconnect() {
        CoreClient.Relay.disconnect { error: Core.Model.Error ->
            Log.d(LOG_TAG, "Failed to disconnect to Wallet Connect: ", error.throwable)
        }
    }

    override fun pair(uri: String) {
        WalletKit.pair(Wallet.Params.Pair(uri), onError = { onPairErrorLiveData.postValue(Event(it.throwable)) })
    }

    private suspend fun handleSessionProposal(proposal: Wallet.Model.SessionProposal) = withContext(Dispatchers.Main) {
        approveSessionRequester.awaitResponse(proposal)
    }

    private suspend fun handleSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
        val sdkSession = interactor.getSession(sessionRequest.topic) ?: run {
            trackSignFailed(sessionRequest, reason = "no_session")
            respondNoSession(sessionRequest)
            return
        }
        val appPairing = interactor.getPairingAccount(sdkSession.pairingTopic) ?: run {
            trackSignFailed(sessionRequest, reason = "no_session")
            respondNoSession(sessionRequest)
            return
        }

        val walletConnectRequest = interactor.parseSessionRequest(sessionRequest)
            .onFailure { error ->
                Log.e("WalletConnect", "Failed to parse session request $sessionRequest", error)

                trackSignFailed(sessionRequest, reason = "unsupported_request")
                respondWithError(sessionRequest, error)

                return
            }.getOrThrow()

        trackSignEvent(sessionRequest) { source, method, chain ->
            AnalyticsEvent.SignRequestShown(source = source, method = method, chain = chain)
        }

        val externalSignResponse = withContext(Dispatchers.Main) {
            dAppSignRequester.awaitConfirmation(
                ExternalSignPayload(
                    signRequest = walletConnectRequest.toExternalSignRequest(),
                    dappMetadata = mapWalletConnectSessionToSignDAppMetadata(sdkSession),
                    wallet = ExternalSignWallet.WithId(appPairing.metaId)
                )
            )
        }

        trackSignOutcome(sessionRequest, externalSignResponse)

        walletConnectRequest.respondWith(externalSignResponse)
    }

    private fun trackSignOutcome(sessionRequest: Wallet.Model.SessionRequest, response: ExternalSignCommunicator.Response) {
        when (response) {
            is ExternalSignCommunicator.Response.Rejected -> trackSignEvent(sessionRequest) { source, method, chain ->
                AnalyticsEvent.SignRejected(source = source, method = method, chain = chain)
            }

            is ExternalSignCommunicator.Response.Signed -> trackSignEvent(sessionRequest) { source, method, chain ->
                AnalyticsEvent.SignApproved(source = source, method = method, chain = chain)
            }

            is ExternalSignCommunicator.Response.SigningFailed -> trackSignFailed(sessionRequest, reason = "signing_failed")

            is ExternalSignCommunicator.Response.Sent -> return
        }
    }

    private fun trackSignFailed(sessionRequest: Wallet.Model.SessionRequest, reason: String) {
        trackSignEvent(sessionRequest) { source, method, chain ->
            AnalyticsEvent.SignFailed(source = source, method = method, chain = chain, reason = reason)
        }
    }

    private inline fun trackSignEvent(
        sessionRequest: Wallet.Model.SessionRequest,
        createEvent: (source: SignSource, method: String, chain: String) -> AnalyticsEvent
    ) {
        val event = createEvent(
            SignSource.WALLET_CONNECT,
            sessionRequest.request.method,
            sessionRequest.chainId ?: UNKNOWN_CHAIN
        )

        analyticsService.track(event)
    }

    private suspend fun handleSessionSettlement(settlement: Wallet.Model.SettledSessionResponse) {
        interactor.onSessionSettled(settlement)
    }

    private suspend fun handleSessionDelete(settlement: Wallet.Model.SessionDelete) {
        interactor.onSessionDelete(settlement)
    }

    private fun mapWalletConnectSessionToSignDAppMetadata(session: Wallet.Model.Session): SigningDappMetadata? {
        return session.metaData?.run {
            SigningDappMetadata(
                icon = icons.firstOrNull(),
                name = name,
                url = url
            )
        }
    }

    private suspend fun respondNoSession(
        sessionRequest: Wallet.Model.SessionRequest,
    ): Result<*> {
        val response = sessionRequest.failed(WalletConnectError.NO_SESSION_FOR_TOPIC)

        return WalletKit.respondSessionRequest(response)
    }

    private suspend fun respondWithError(
        sessionRequest: Wallet.Model.SessionRequest,
        exception: Throwable
    ): Result<*> {
        val error = exception as? WalletConnectError ?: WalletConnectError.GENERAL_FAILURE
        val response = sessionRequest.failed(error)

        return WalletKit.respondSessionRequest(response)
    }
}
