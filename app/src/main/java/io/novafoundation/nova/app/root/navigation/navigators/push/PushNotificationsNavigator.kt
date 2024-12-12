package io.novafoundation.nova.app.root.navigation.navigators.push

import android.os.Bundle
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter

class PushNotificationsNavigator(
    splitScreenNavigationHolder: SplitScreenNavigationHolder,
    rootNavigationHolder: RootNavigationHolder
) : BaseNavigator(splitScreenNavigationHolder, rootNavigationHolder), PushNotificationsRouter {

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
