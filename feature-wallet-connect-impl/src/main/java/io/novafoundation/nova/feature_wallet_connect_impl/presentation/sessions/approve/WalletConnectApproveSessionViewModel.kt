package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve

import android.util.Log
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.navigation.requireLastInput
import io.novafoundation.nova.common.navigation.respond
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.selectedMetaAccount
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WalletConnectApproveSessionViewModel(
    private val router: WalletConnectRouter,
    private val interactor: WalletConnectSessionInteractor,
    private val responder: ApproveSessionResponder,
    private val resourceManager: ResourceManager,
    private val selectWalletMixinFactory: SelectWalletMixin.Factory
) : BaseViewModel() {

    val selectWalletMixin = selectWalletMixinFactory.create(this)

    private val sessionProposalFlow = flowOf {
        interactor.resolveSessionProposal(responder.requireLastInput())
    }.shareInBackground()

    val sessionMetadata = sessionProposalFlow.map { it.dappMetadata }

    val title = sessionMetadata.map { sessionDAppMetadata ->
        val dAppTitle = sessionDAppMetadata.name ?: sessionDAppMetadata.dappUrl

        resourceManager.getString(R.string.dapp_confirm_authorize_title_format, dAppTitle)
    }.shareInBackground()

    fun exit() {
        rejectClicked()
    }

    fun rejectClicked() = launch {
        val proposal = responder.requireLastInput()

        interactor.rejectSession(proposal)
        responder.respond()
        router.back()
    }

    fun approveClicked() = launch{
        val proposal = responder.requireLastInput()
        val metaAccount = selectWalletMixin.selectedMetaAccount()

        interactor.approveSession(proposal, metaAccount)
            .onFailure {
                Log.d("WalletConnect", "Session approve failed", it)
            }

        responder.respond()
        router.back()
    }

    fun networksClicked() {
    }
}
