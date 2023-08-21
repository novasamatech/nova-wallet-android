package io.novafoundation.nova.app.di.app.navigation.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.staking.StartMultiStakingNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
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
    fun provideStartMultiStakingRouter(navigationHolder: NavigationHolder): StartMultiStakingRouter {
        return StartMultiStakingNavigator(navigationHolder)
    }
}
