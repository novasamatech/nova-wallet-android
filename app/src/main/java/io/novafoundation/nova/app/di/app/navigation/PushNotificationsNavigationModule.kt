package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.push.PushGovernanceSettingsCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.push.PushNotificationsNavigator
import io.novafoundation.nova.app.root.navigation.navigators.push.PushStakingSettingsCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.staking.PushStakingSettingsCommunicator

@Module
class PushNotificationsNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHolder: SplitScreenNavigationHolder): PushNotificationsRouter = PushNotificationsNavigator(navigationHolder)

    @Provides
    @ApplicationScope
    fun providePushGovernanceSettingsCommunicator(
        router: PushNotificationsRouter,
        navigationHolder: SplitScreenNavigationHolder
    ): PushGovernanceSettingsCommunicator = PushGovernanceSettingsCommunicatorImpl(router, navigationHolder)

    @Provides
    @ApplicationScope
    fun providePushStakingSettingsCommunicator(
        router: PushNotificationsRouter,
        navigationHolder: SplitScreenNavigationHolder
    ): PushStakingSettingsCommunicator = PushStakingSettingsCommunicatorImpl(router, navigationHolder)
}
