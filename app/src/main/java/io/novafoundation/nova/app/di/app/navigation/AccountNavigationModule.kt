package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.app.root.navigation.account.PolkadotVaultVariantSignCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.account.SelectAddressCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.account.SelectMultipleWalletsCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.account.SelectWalletCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.pincode.PinCodeTwoFactorVerificationCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter

@Module
class AccountNavigationModule {

    @Provides
    @ApplicationScope
    fun providePinCodeTwoFactorVerificationCommunicator(
        navigationHolder: NavigationHolder
    ): PinCodeTwoFactorVerificationCommunicator = PinCodeTwoFactorVerificationCommunicatorImpl(navigationHolder)

    @Provides
    @ApplicationScope
    fun provideSelectWalletCommunicator(
        navigationHolder: NavigationHolder
    ): SelectWalletCommunicator = SelectWalletCommunicatorImpl(navigationHolder)

    @Provides
    @ApplicationScope
    fun provideParitySignerCommunicator(
        navigationHolder: NavigationHolder
    ): PolkadotVaultVariantSignCommunicator = PolkadotVaultVariantSignCommunicatorImpl(navigationHolder)

    @Provides
    @ApplicationScope
    fun provideSelectAddressCommunicator(
        router: AssetsRouter,
        navigationHolder: NavigationHolder
    ): SelectAddressCommunicator = SelectAddressCommunicatorImpl(router, navigationHolder)

    @Provides
    @ApplicationScope
    fun provideSelectMultipleWalletsCommunicator(
        router: AssetsRouter,
        navigationHolder: NavigationHolder
    ): SelectMultipleWalletsCommunicator = SelectMultipleWalletsCommunicatorImpl(router, navigationHolder)

    @ApplicationScope
    @Provides
    fun provideAccountRouter(navigator: Navigator): AccountRouter = navigator
}
