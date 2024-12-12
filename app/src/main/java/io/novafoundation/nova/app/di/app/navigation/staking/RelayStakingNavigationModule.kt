package io.novafoundation.nova.app.di.app.navigation.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain.RelayStakingNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter

@Module
class RelayStakingNavigationModule {

    @Provides
    @ApplicationScope
    fun provideRelayStakingRouter(
        navigationHolder: SplitScreenNavigationHolder,
        navigator: Navigator,
        dashboardRouter: StakingDashboardRouter
    ): StakingRouter {
        return RelayStakingNavigator(navigationHolder, navigator, dashboardRouter)
    }
}
