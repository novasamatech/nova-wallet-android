package io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions

import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignRequester
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.awaitConfirmation
import io.novafoundation.nova.feature_dapp_impl.walletConnect.WalletConnectScanCommunicator
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.session.KnownSessionRequest
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.PolkadotJsSignRequest
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayload
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.AuthorizeDAppPayload
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class WalletConnectSessionsViewModel(
    private val router: DAppRouter,
    private val scanCommunicator: WalletConnectScanCommunicator,
    private val awaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val walletUiUseCase: WalletUiUseCase,
    private val interactor: WalletConnectSessionInteractor,
    private val dAppSignRequester: DAppSignRequester,
) : BaseViewModel() {

    val authorizeDapp = awaitableMixinFactory.confirmingOrDenyingAction<AuthorizeDAppPayload>()

    private val selectedWalletUiFlow = walletUiUseCase.selectedWalletUiFlow(showAddressIcon = true)
        .shareInBackground()

    init {
        CoreClient.Relay.connect { _: Core.Model.Error -> }
    }

    val events = Web3Wallet.sessionEventsFlow(scope = this)

    init {
        events.onEach {
            when (it) {
                is WalletConnectSessionsEvent.SessionProposal -> handleSessionProposal(it.proposal)
                is WalletConnectSessionsEvent.SessionRequest -> handleSessionRequest(it.request)
            }
        }.launchIn(this)

        scanCommunicator.responseFlow.onEach {
            Web3Wallet.pair(Wallet.Params.Pair(it.wcUri), onError = { showError(it.throwable) })
        }
            .launchIn(this)
    }

    fun exit() {
        router.back()
    }

    private suspend fun handleSessionProposal(proposal: Wallet.Model.SessionProposal) {
        val payload = AuthorizeDAppPayload(
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
        // TODO reject request if not able to parse
        val knownSessionRequest = interactor.parseSessionRequest(sessionRequest).getOrNull() ?: return
        val confirmTxRequest = mapKnownSessionRequestToTxRequest(sessionRequest.request.id.toString(), knownSessionRequest)

        val result = dAppSignRequester.awaitConfirmation(DAppSignPayload(confirmTxRequest, dappUrl = null))

        interactor.respondSessionRequest(knownSessionRequest, result)
    }

    private fun mapKnownSessionRequestToTxRequest(requestId: String, request: KnownSessionRequest): ConfirmTxRequest {
        return when (val params = request.params) {
            is KnownSessionRequest.Params.Polkadot.SignMessage -> {
                PolkadotJsSignRequest(requestId, SignerPayload.Raw(data = params.message, address = params.address, type = null))
            }
            is KnownSessionRequest.Params.Polkadot.SignTransaction -> {
                PolkadotJsSignRequest(requestId, params.transactionPayload)
            }
        }
    }
}
