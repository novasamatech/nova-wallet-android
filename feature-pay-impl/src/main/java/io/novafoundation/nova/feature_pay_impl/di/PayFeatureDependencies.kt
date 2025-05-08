package io.novafoundation.nova.feature_pay_impl.di

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mixin.WalletConnectSessionsMixinFactory

interface PayFeatureDependencies {

    val selectedAccountUseCase: SelectedAccountUseCase

    val walletConnectSessionsMixinFactory: WalletConnectSessionsMixinFactory

    val accountRepository: AccountRepository
}
