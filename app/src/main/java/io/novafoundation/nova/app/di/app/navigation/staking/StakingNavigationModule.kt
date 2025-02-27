package io.novafoundation.nova.app.di.app.navigation.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
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
    fun provideStakingDashboardNavigator(navigationHoldersRegistry: NavigationHoldersRegistry): StakingDashboardNavigator {
        return StakingDashboardNavigator(navigationHoldersRegistry)
    }

    @Provides
    @ApplicationScope
    fun provideStakingDashboardRouter(relayStakingNavigator: StakingDashboardNavigator): StakingDashboardRouter = relayStakingNavigator

    @Provides
    @ApplicationScope
    fun provideStartMultiStakingRouter(
        navigationHoldersRegistry: NavigationHoldersRegistry,
        dashboardRouter: StakingDashboardRouter,
        commonNavigator: Navigator
    ): StartMultiStakingRouter {
        return StartMultiStakingNavigator(navigationHoldersRegistry, dashboardRouter, commonNavigator)
    }
}
