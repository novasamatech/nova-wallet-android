package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.account.PolkadotVaultVariantSignCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.account.SelectAddressCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.account.SelectMultipleWalletsCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.account.SelectWalletCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.cloudBackup.ChangeBackupPasswordCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.cloudBackup.RestoreBackupPasswordCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.cloudBackup.SyncWalletsBackupPasswordCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.pincode.PinCodeTwoFactorVerificationCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.sequrity.verification.PinCodeTwoFactorVerificationCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.ChangeBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.changePassword.RestoreBackupPasswordCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.cloudBackup.createPassword.SyncWalletsBackupPasswordCommunicator
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter

@Module
class AccountNavigationModule {

    @Provides
    @ApplicationScope
    fun providePinCodeTwoFactorVerificationCommunicator(
        navigationHolder: MainNavigationHolder
    ): PinCodeTwoFactorVerificationCommunicator = PinCodeTwoFactorVerificationCommunicatorImpl(navigationHolder)

    @Provides
    @ApplicationScope
    fun provideSelectWalletCommunicator(
        navigationHolder: MainNavigationHolder
    ): SelectWalletCommunicator = SelectWalletCommunicatorImpl(navigationHolder)

    @Provides
    @ApplicationScope
    fun provideParitySignerCommunicator(
        navigationHolder: MainNavigationHolder
    ): PolkadotVaultVariantSignCommunicator = PolkadotVaultVariantSignCommunicatorImpl(navigationHolder)

    @Provides
    @ApplicationScope
    fun provideSelectAddressCommunicator(
        router: AssetsRouter,
        navigationHolder: MainNavigationHolder
    ): SelectAddressCommunicator = SelectAddressCommunicatorImpl(router, navigationHolder)

    @Provides
    @ApplicationScope
    fun provideSelectMultipleWalletsCommunicator(
        router: AssetsRouter,
        navigationHolder: MainNavigationHolder
    ): SelectMultipleWalletsCommunicator = SelectMultipleWalletsCommunicatorImpl(router, navigationHolder)

    @ApplicationScope
    @Provides
    fun provideAccountRouter(navigator: Navigator): AccountRouter = navigator

    @Provides
    @ApplicationScope
    fun providePushGovernanceSettingsCommunicator(
        router: AccountRouter,
        navigationHolder: MainNavigationHolder
    ): SyncWalletsBackupPasswordCommunicator = SyncWalletsBackupPasswordCommunicatorImpl(router, navigationHolder)

    @Provides
    @ApplicationScope
    fun provideChangeBackupPasswordCommunicator(
        router: AccountRouter,
        navigationHolder: MainNavigationHolder
    ): ChangeBackupPasswordCommunicator = ChangeBackupPasswordCommunicatorImpl(router, navigationHolder)

    @Provides
    @ApplicationScope
    fun provideRestoreBackupPasswordCommunicator(
        router: AccountRouter,
        navigationHolder: MainNavigationHolder
    ): RestoreBackupPasswordCommunicator = RestoreBackupPasswordCommunicatorImpl(router, navigationHolder)
}
