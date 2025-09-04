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
        navigationBuilder().action(R.id.action_pushWelcome_to_pushSettings)
            .navigateInFirstAttachedContext()
    }

    override fun openPushGovernanceSettings(args: Bundle) {
        navigationBuilder().action(R.id.action_pushSettings_to_governanceSettings)
            .setArgs(args)
            .navigateInFirstAttachedContext()
    }

    override fun openPushStakingSettings(args: Bundle) {
        navigationBuilder().action(R.id.action_pushSettings_to_stakingSettings)
            .setArgs(args)
            .navigateInFirstAttachedContext()
    }
}
