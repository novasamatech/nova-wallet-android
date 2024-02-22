package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.governance.SelectTracksCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.push.PushGovernanceSettingsCommunicatorImpl
import io.novafoundation.nova.app.root.navigation.push.PushNotificationsNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsCommunicator

@Module
class PushNotificationsNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHolder: NavigationHolder): PushNotificationsRouter = PushNotificationsNavigator(navigationHolder)

    @Provides
    @ApplicationScope
    fun provideSelectTracksCommunicator(
        router: PushNotificationsRouter,
        navigationHolder: NavigationHolder
    ): PushGovernanceSettingsCommunicator = PushGovernanceSettingsCommunicatorImpl(router, navigationHolder)
}
