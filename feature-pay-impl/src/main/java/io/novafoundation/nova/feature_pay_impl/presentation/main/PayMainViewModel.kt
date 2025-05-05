package io.novafoundation.nova.feature_pay_impl.presentation.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mixin.WalletConnectSessionsMixinFactory

class PayMainViewModel(
    private val router: PayRouter,
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
