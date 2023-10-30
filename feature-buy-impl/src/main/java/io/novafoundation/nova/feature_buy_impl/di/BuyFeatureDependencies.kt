package io.novafoundation.nova.feature_buy_impl.di

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository

interface BuyFeatureDependencies {

    val resourceManager: ResourceManager

    val accountRepository: AccountRepository

    val walletRepository: WalletRepository
}
