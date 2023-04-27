package io.novafoundation.nova.feature_wallet_connect_impl.presentation.service

import android.util.Log
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.common.navigation.awaitResponse
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.inBackground
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

internal class RealWalletConnectServiceFactory(
    private val interactor: WalletConnectSessionInteractor,
    private val dAppSignRequester: ExternalSignRequester,
    private val approveSessionRequester: ApproveSessionRequester,
) : WalletConnectService.Factory {

    override fun create(coroutineScope: CoroutineScope): WalletConnectService {
        return RealWalletConnectService(
            parentScope = coroutineScope,
            interactor = interactor,
            dAppSignRequester = dAppSignRequester,
            approveSessionRequester = approveSessionRequester
        )
    }
}

internal class RealWalletConnectService(
    parentScope: CoroutineScope,
    private val interactor: WalletConnectSessionInteractor,
    private val dAppSignRequester: ExternalSignRequester,
    private val approveSessionRequester: ApproveSessionRequester,
) : WalletConnectService,
    CoroutineScope by parentScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(parentScope) {

    private val events = Web3Wallet.sessionEventsFlow(scope = this)

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

    private suspend fun handleSessionProposal(proposal: Wallet.Model.SessionProposal) = withContext(Dispatchers.Main) {
        approveSessionRequester.awaitResponse(proposal)
    }

    private suspend fun handleSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
        val sdkSession = Web3Wallet.getActiveSessionByTopic(sessionRequest.topic) ?: run { respondNoSession(sessionRequest); return }
        val appSession = interactor.getSessionAccount(sessionRequest.topic) ?: run { respondNoSession(sessionRequest); return }

        val walletConnectRequest = interactor.parseSessionRequest(sessionRequest)
            .onFailure { Log.e("WalletConnect", "Failed to parse session request $sessionRequest", it) }
            .getOrNull()
            ?: run { respondUnauthorizedMethod(sessionRequest); return }

        val externalSignResponse = withContext(Dispatchers.Main) {
            dAppSignRequester.awaitConfirmation(
                ExternalSignPayload(
                    signRequest = walletConnectRequest.toExternalSignRequest(),
                    dappMetadata = mapWalletConnectSessionToSignDAppMetadata(sdkSession),
                    wallet = ExternalSignWallet.WithId(appSession.metaId)
                )
            )
        }

        walletConnectRequest.respondWith(externalSignResponse)
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

        return Web3Wallet.respondSessionRequest(response)
    }

    private suspend fun respondUnauthorizedMethod(
        sessionRequest: Wallet.Model.SessionRequest,
    ): Result<*> {
        val response = sessionRequest.failed(WalletConnectError.UNAUTHORIZED_METHOD)

        return Web3Wallet.respondSessionRequest(response)
    }
}
