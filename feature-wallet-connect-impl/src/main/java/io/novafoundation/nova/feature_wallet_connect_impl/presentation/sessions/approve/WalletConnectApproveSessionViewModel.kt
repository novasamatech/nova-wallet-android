package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve

import android.util.Log
import androidx.annotation.StringRes
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.navigation.requireLastInput
import io.novafoundation.nova.common.navigation.respond
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.selectedMetaAccount
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val processState = MutableStateFlow(ProgressState.IDLE)

    val allowButtonState = buttonStateFor(ProgressState.CONFIRMING, R.string.common_allow)

    val rejectButtonState = buttonStateFor(ProgressState.REJECTING, R.string.common_reject)

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
        if (isInProgress()) return@launch
        processState.value = ProgressState.REJECTING

        val proposal = responder.requireLastInput()

        interactor.rejectSession(proposal)
        responder.respond()
        router.back()
    }

    fun approveClicked() = launch {
        if (isInProgress()) return@launch
        processState.value = ProgressState.CONFIRMING

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

    private fun buttonStateFor(
        buttonAction: ProgressState,
        @StringRes idleLabelRes: Int
    ): Flow<DescriptiveButtonState> {
        return processState.map { progressState ->
            when (progressState) {
                ProgressState.IDLE -> DescriptiveButtonState.Enabled(
                    resourceManager.getString(idleLabelRes)
                )

                buttonAction -> DescriptiveButtonState.Loading

                else -> DescriptiveButtonState.Disabled(resourceManager.getString(idleLabelRes))
            }
        }
            .shareInBackground()
    }

    private fun isInProgress(): Boolean {
        return processState.value != ProgressState.IDLE
    }
}

private enum class ProgressState {
    IDLE, CONFIRMING, REJECTING
}
