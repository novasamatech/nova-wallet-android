package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.staking.ParachainStakingNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter

@Module
class StakingNavigationModule {

    @Provides
    @ApplicationScope
    fun provideParachainStakingRouter(navigationHolder: NavigationHolder): ParachainStakingRouter = ParachainStakingNavigator(navigationHolder)
}
