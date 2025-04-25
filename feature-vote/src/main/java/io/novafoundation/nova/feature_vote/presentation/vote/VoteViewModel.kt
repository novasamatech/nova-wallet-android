package io.novafoundation.nova.feature_vote.presentation.vote

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_vote.presentation.VoteRouter
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mixin.WalletConnectSessionsMixinFactory

class VoteViewModel(
    private val router: VoteRouter,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val walletConnectSessionsMixinFactory: WalletConnectSessionsMixinFactory
) : BaseViewModel() {

    val selectedWalletModel = selectedAccountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    private val walletConnectSessionsMixin = walletConnectSessionsMixinFactory.create(this)

    val walletConnectAccountSessions = walletConnectSessionsMixin.getActiveSessionsForSelectedAccount()

    fun avatarClicked() {
        router.openSwitchWallet()
    }

    fun walletConnectClicked() {
        walletConnectSessionsMixin.onWalletConnectClick()
    }
}
