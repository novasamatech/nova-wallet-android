package io.novafoundation.nova.app.root.navigation.navigators.push

import android.os.Bundle
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter

class PushNotificationsNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry
) : BaseNavigator(navigationHoldersRegistry), PushNotificationsRouter {

    override fun openPushSettings() {
        navigationBuilder(R.id.action_pushWelcome_to_pushSettings)
            .perform()
    }

    override fun openPushGovernanceSettings(args: Bundle) {
        navigationBuilder(R.id.action_pushSettings_to_governanceSettings)
            .setArgs(args)
            .perform()
    }

    override fun openPushStakingSettings(args: Bundle) {
        navigationBuilder(R.id.action_pushSettings_to_stakingSettings)
            .setArgs(args)
            .perform()
    }
}
