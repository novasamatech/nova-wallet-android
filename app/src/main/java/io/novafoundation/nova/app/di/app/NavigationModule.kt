package io.novafoundation.nova.app.di.app

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.Navigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.splash.SplashRouter

@Module
class NavigationModule {

    @ApplicationScope
    @Provides
    fun provideNavigator(): Navigator = Navigator()

    @ApplicationScope
    @Provides
    fun provideSplashRouter(navigator: Navigator): SplashRouter = navigator

    @ApplicationScope
    @Provides
    fun provideOnboardingRouter(navigator: Navigator): OnboardingRouter = navigator

    @ApplicationScope
    @Provides
    fun provideAccountRouter(navigator: Navigator): AccountRouter = navigator

    @ApplicationScope
    @Provides
    fun provideWalletRouter(navigator: Navigator): WalletRouter = navigator

    @ApplicationScope
    @Provides
    fun provideStakingRouter(navigator: Navigator): StakingRouter = navigator

    @ApplicationScope
    @Provides
    fun provideCrowdloanRouter(navigator: Navigator): CrowdloanRouter = navigator
}
