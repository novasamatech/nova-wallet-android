package io.novafoundation.nova.app.di.app.navigation.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.staking.mythos.MythosStakingNavigator
import io.novafoundation.nova.app.root.navigation.navigators.staking.mythos.SelectMythosCollatorInterScreenCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.SelectMythosInterScreenCommunicator

@Module
class MythosStakingNavigationModule {

    @Provides
    @ApplicationScope
    fun provideMythosStakingRouter(
        navigationHoldersRegistry: NavigationHoldersRegistry,
    ): MythosStakingRouter {
        return MythosStakingNavigator(navigationHoldersRegistry)
    }

    @Provides
    @ApplicationScope
    fun provideSelectCollatorCommunicator(navigationHoldersRegistry: NavigationHoldersRegistry): SelectMythosInterScreenCommunicator {
        return SelectMythosCollatorInterScreenCommunicatorImpl(navigationHoldersRegistry)
    }
}
