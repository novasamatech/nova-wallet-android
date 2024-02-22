package io.novafoundation.nova.app.root.navigation.push

import android.os.Bundle
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter

class PushNotificationsNavigator(
    navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), PushNotificationsRouter {

    override fun openPushSettings() {
        performNavigation(R.id.action_pushWelcome_to_pushSettings)
    }

    override fun openPushGovernanceSettings(args: Bundle) {
        performNavigation(R.id.action_pushSettings_to_governanceSettings, args)
    }
}
