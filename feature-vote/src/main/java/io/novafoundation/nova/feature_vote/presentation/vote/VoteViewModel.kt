package io.novafoundation.nova.feature_vote.presentation.vote

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_vote.presentation.VoteRouter

class VoteViewModel(
    private val router: VoteRouter,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : BaseViewModel() {

    val selectedWalletModel = selectedAccountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    fun avatarClicked() {
        router.openSwitchWallet()
    }
}
