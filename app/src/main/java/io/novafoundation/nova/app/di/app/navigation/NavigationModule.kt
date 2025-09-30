package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.di.app.navigation.staking.StakingNavigationModule
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.navigation.DelayedNavigationRouter
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.splash.SplashRouter

@Module(
    includes = [
        AccountNavigationModule::class,
        AssetNavigationModule::class,
        DAppNavigationModule::class,
        NftNavigationModule::class,
        StakingNavigationModule::class,
        LedgerNavigationModule::class,
        CurrencyNavigationModule::class,
        GovernanceNavigationModule::class,
        WalletConnectNavigationModule::class,
        VoteNavigationModule::class,
        VersionsNavigationModule::class,
        ExternalSignNavigationModule::class,
        SettingsNavigationModule::class,
        SwapNavigationModule::class,
        BuyNavigationModule::class,
        PushNotificationsNavigationModule::class,
        CloudBackupNavigationModule::class,
        AssetNavigationModule::class,
        AccountMigrationNavigationModule::class,
        MultisigNavigationModule::class,
        ChainMigrationNavigationModule::class
    ]
)
class NavigationModule {

    @ApplicationScope
    @Provides
    fun provideMainNavigatorHolder(
        contextManager: ContextManager
    ): SplitScreenNavigationHolder = SplitScreenNavigationHolder(contextManager)

    @ApplicationScope
    @Provides
    fun provideDappNavigatorHolder(
        contextManager: ContextManager
    ): RootNavigationHolder = RootNavigationHolder(contextManager)

    @ApplicationScope
    @Provides
    fun provideNavigationHoldersRegistry(
        rootNavigatorHolder: RootNavigationHolder,
        splitScreenNavigationHolder: SplitScreenNavigationHolder,
    ): NavigationHoldersRegistry {
        return NavigationHoldersRegistry(splitScreenNavigationHolder, rootNavigatorHolder)
    }

    @ApplicationScope
    @Provides
    fun provideNavigator(
        navigationHoldersRegistry: NavigationHoldersRegistry,
        walletConnectRouter: WalletConnectRouter
    ): Navigator = Navigator(navigationHoldersRegistry, walletConnectRouter)

    @Provides
    @ApplicationScope
    fun provideRootRouter(navigator: Navigator): RootRouter = navigator

    @ApplicationScope
    @Provides
    fun provideSplashRouter(navigator: Navigator): SplashRouter = navigator

    @ApplicationScope
    @Provides
    fun provideOnboardingRouter(navigator: Navigator): OnboardingRouter = navigator

    @ApplicationScope
    @Provides
    fun provideAssetsRouter(navigator: Navigator): AssetsRouter = navigator

    @ApplicationScope
    @Provides
    fun provideCrowdloanRouter(navigator: Navigator): CrowdloanRouter = navigator

    @ApplicationScope
    @Provides
    fun provideDelayedNavigationRouter(navigator: Navigator): DelayedNavigationRouter = navigator
}
