package io.novafoundation.nova.app.root.navigation.navigators.push

import android.os.Bundle
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter

class PushNotificationsNavigator(
    navigationHolder: SplitScreenNavigationHolder,
) : BaseNavigator(navigationHolder), PushNotificationsRouter {

    override fun openPushSettings() {
        performNavigation(R.id.action_pushWelcome_to_pushSettings)
    }

    override fun openPushGovernanceSettings(args: Bundle) {
        performNavigation(R.id.action_pushSettings_to_governanceSettings, args)
    }

    override fun openPushStakingSettings(args: Bundle) {
        performNavigation(R.id.action_pushSettings_to_stakingSettings, args)
    }
}
