package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions

import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignRequester
import io.novafoundation.nova.feature_external_sign_api.model.awaitConfirmation
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.SigningDappMetadata
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.PolkadotSignPayload
import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectScanCommunicator
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.KnownSessionRequest
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class WalletConnectSessionsViewModel(
    private val router: WalletConnectRouter,
    private val scanCommunicator: WalletConnectScanCommunicator,
    private val awaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val walletUiUseCase: WalletUiUseCase,
    private val interactor: WalletConnectSessionInteractor,
    private val dAppSignRequester: ExternalSignRequester,
) : BaseViewModel() {

    val authorizeDapp = awaitableMixinFactory.confirmingOrDenyingAction<AuthorizeDappBottomSheet.Payload>()

    private val selectedWalletUiFlow = walletUiUseCase.selectedWalletUiFlow(showAddressIcon = true)
        .shareInBackground()

    init {
        CoreClient.Relay.connect { _: Core.Model.Error -> }
    }

    private val events = Web3Wallet.sessionEventsFlow(scope = this)

    init {
        events.onEach {
            when (it) {
                is WalletConnectSessionsEvent.SessionProposal -> handleSessionProposal(it.proposal)
                is WalletConnectSessionsEvent.SessionRequest -> handleSessionRequest(it.request)
            }
        }
            .inBackground()
            .launchIn(this)

        scanCommunicator.responseFlow.onEach {
            Web3Wallet.pair(Wallet.Params.Pair(it.wcUri), onError = { showError(it.throwable) })
        }
            .launchIn(this)
    }

    fun exit() {
        router.back()
    }

    private suspend fun handleSessionProposal(proposal: Wallet.Model.SessionProposal) {
        val payload = AuthorizeDappBottomSheet.Payload(
            title = proposal.name,
            dAppIconUrl = proposal.icons.firstOrNull()?.toString(),
            dAppUrl = proposal.url,
            walletModel = selectedWalletUiFlow.first()
        )

        val allowed = authorizeDapp.awaitAction(payload)

        if (allowed) {
            interactor.approveSession(proposal)
        } else {
            interactor.rejectSession(proposal)
        }
    }

    override fun onCleared() {
        super.onCleared()

        CoreClient.Relay.disconnect { _: Core.Model.Error -> }
    }

    fun initiateScan() {
        scanCommunicator.openRequest(WalletConnectScanCommunicator.Request())
    }

    private suspend fun handleSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
        val session = Web3Wallet.getActiveSessionByTopic(sessionRequest.topic) ?: return

        // TODO reject request if not able to parse
        val knownSessionRequest = interactor.parseSessionRequest(sessionRequest).getOrNull() ?: return
        val confirmTxRequest = mapKnownSessionRequestToExternalSignRequest(sessionRequest.request.id.toString(), knownSessionRequest)

        val result = withContext(Dispatchers.Main) {
            dAppSignRequester.awaitConfirmation(
                ExternalSignPayload(
                    signRequest = confirmTxRequest,
                    dappMetadata = mapWalletConnectSessionToSignDAppMetadata(session)
                )
            )
        }

        interactor.respondSessionRequest(knownSessionRequest, result)
    }

    private fun mapKnownSessionRequestToExternalSignRequest(requestId: String, request: KnownSessionRequest): ExternalSignRequest {
        return when (val params = request.params) {
            is KnownSessionRequest.Params.Polkadot.SignMessage -> {
                ExternalSignRequest.Polkadot(requestId, PolkadotSignPayload.Raw(data = params.message, address = params.address, type = null))
            }
            is KnownSessionRequest.Params.Polkadot.SignTransaction -> {
                ExternalSignRequest.Polkadot(requestId, params.transactionPayload)
            }
        }
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
}
