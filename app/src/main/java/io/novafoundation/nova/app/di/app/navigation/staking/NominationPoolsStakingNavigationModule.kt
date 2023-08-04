package io.novafoundation.nova.app.di.app.navigation.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.staking.nominationPools.NominationPoolsStakingNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter

@Module
class NominationPoolsStakingNavigationModule {

    @Provides
    @ApplicationScope
    fun provideRouter(navigationHolder: NavigationHolder): NominationPoolsRouter {
        return NominationPoolsStakingNavigator(navigationHolder)
    }
}
