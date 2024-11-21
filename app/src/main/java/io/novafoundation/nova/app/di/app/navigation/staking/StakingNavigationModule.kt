package io.novafoundation.nova.app.di.app.navigation.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.staking.StakingDashboardNavigator
import io.novafoundation.nova.app.root.navigation.navigators.staking.StartMultiStakingNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter

@Module(
    includes = [
        ParachainStakingNavigationModule::class,
        RelayStakingNavigationModule::class,
        NominationPoolsStakingNavigationModule::class
    ]
)
class StakingNavigationModule {

    @Provides
    @ApplicationScope
    fun provideStakingDashboardNavigator(navigationHolder: MainNavigationHolder): StakingDashboardNavigator {
        return StakingDashboardNavigator(navigationHolder)
    }

    @Provides
    @ApplicationScope
    fun provideStakingDashboardRouter(relayStakingNavigator: StakingDashboardNavigator): StakingDashboardRouter = relayStakingNavigator

    @Provides
    @ApplicationScope
    fun provideStartMultiStakingRouter(
        navigationHolder: MainNavigationHolder,
        dashboardRouter: StakingDashboardRouter,
        commonNavigator: Navigator
    ): StartMultiStakingRouter {
        return StartMultiStakingNavigator(navigationHolder, dashboardRouter, commonNavigator)
    }
}
