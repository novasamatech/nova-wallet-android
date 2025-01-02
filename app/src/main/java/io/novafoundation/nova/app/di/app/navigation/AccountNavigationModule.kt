package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
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
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): PinCodeTwoFactorVerificationCommunicator = PinCodeTwoFactorVerificationCommunicatorImpl(navigationHoldersRegistry)

    @Provides
    @ApplicationScope
    fun provideSelectWalletCommunicator(
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): SelectWalletCommunicator = SelectWalletCommunicatorImpl(navigationHoldersRegistry)

    @Provides
    @ApplicationScope
    fun provideParitySignerCommunicator(
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): PolkadotVaultVariantSignCommunicator = PolkadotVaultVariantSignCommunicatorImpl(navigationHoldersRegistry)

    @Provides
    @ApplicationScope
    fun provideSelectAddressCommunicator(
        router: AssetsRouter,
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): SelectAddressCommunicator = SelectAddressCommunicatorImpl(router, navigationHoldersRegistry)

    @Provides
    @ApplicationScope
    fun provideSelectMultipleWalletsCommunicator(
        router: AssetsRouter,
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): SelectMultipleWalletsCommunicator = SelectMultipleWalletsCommunicatorImpl(router, navigationHoldersRegistry)

    @ApplicationScope
    @Provides
    fun provideAccountRouter(navigator: Navigator): AccountRouter = navigator

    @Provides
    @ApplicationScope
    fun providePushGovernanceSettingsCommunicator(
        router: AccountRouter,
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): SyncWalletsBackupPasswordCommunicator = SyncWalletsBackupPasswordCommunicatorImpl(router, navigationHoldersRegistry)

    @Provides
    @ApplicationScope
    fun provideChangeBackupPasswordCommunicator(
        router: AccountRouter,
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): ChangeBackupPasswordCommunicator = ChangeBackupPasswordCommunicatorImpl(router, navigationHoldersRegistry)

    @Provides
    @ApplicationScope
    fun provideRestoreBackupPasswordCommunicator(
        router: AccountRouter,
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): RestoreBackupPasswordCommunicator = RestoreBackupPasswordCommunicatorImpl(router, navigationHoldersRegistry)
}
