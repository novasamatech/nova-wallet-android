package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.app.root.navigation.navigators.push.PushGovernanceSettingsCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.push.PushMultisigSettingsCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.navigators.push.PushNotificationsNavigator
import io.novafoundation.nova.app.root.navigation.navigators.push.PushStakingSettingsCommunicatorImpl
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.staking.PushStakingSettingsCommunicator

@Module
class PushNotificationsNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHoldersRegistry: NavigationHoldersRegistry): PushNotificationsRouter =
        PushNotificationsNavigator(navigationHoldersRegistry)

    @Provides
    @ApplicationScope
    fun providePushGovernanceSettingsCommunicator(
        router: PushNotificationsRouter,
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): PushGovernanceSettingsCommunicator = PushGovernanceSettingsCommunicatorImpl(router, navigationHoldersRegistry)

    @Provides
    @ApplicationScope
    fun providePushStakingSettingsCommunicator(
        router: PushNotificationsRouter,
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): PushStakingSettingsCommunicator = PushStakingSettingsCommunicatorImpl(router, navigationHoldersRegistry)

    @Provides
    @ApplicationScope
    fun providePushMultisigSettingsCommunicator(
        router: PushNotificationsRouter,
        navigationHoldersRegistry: NavigationHoldersRegistry
    ): PushMultisigSettingsCommunicator = PushMultisigSettingsCommunicatorImpl(router, navigationHoldersRegistry)
}
