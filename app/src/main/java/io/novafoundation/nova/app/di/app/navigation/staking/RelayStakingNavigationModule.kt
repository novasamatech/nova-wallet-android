package io.novafoundation.nova.app.di.app.navigation.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain.RelayStakingNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter

@Module
class RelayStakingNavigationModule {

    @Provides
    @ApplicationScope
    fun provideRelayStakingRouter(
        navigationHoldersRegistry: NavigationHoldersRegistry,
        navigator: Navigator,
        dashboardRouter: StakingDashboardRouter,
        dAppRouter: DAppRouter
    ): StakingRouter {
        return RelayStakingNavigator(navigationHoldersRegistry, navigator, dashboardRouter, dAppRouter)
    }
}
