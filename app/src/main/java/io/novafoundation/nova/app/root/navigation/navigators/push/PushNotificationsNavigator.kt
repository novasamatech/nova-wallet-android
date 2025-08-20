package io.novafoundation.nova.app.root.navigation.navigators.push

import android.os.Bundle
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.presentation.settings.PushSettingsFragment
import io.novafoundation.nova.feature_push_notifications.presentation.settings.PushSettingsPayload
import io.novafoundation.nova.feature_push_notifications.presentation.settings.withWalletSelection

class PushNotificationsNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry
) : BaseNavigator(navigationHoldersRegistry), PushNotificationsRouter {

    override fun openPushSettingsWithAccounts() {
        navigationBuilder().action(R.id.action_open_pushNotificationsSettings)
            .setArgs(PushSettingsFragment.createPayload(PushSettingsPayload.withWalletSelection(enableSwitcherOnStart = true)))
            .navigateInFirstAttachedContext()
    }

    override fun openPushMultisigsSettings(args: Bundle) {
        navigationBuilder().action(R.id.action_pushSettings_to_multisigSettings)
            .setArgs(args)
            .navigateInFirstAttachedContext()
    }

    override fun openPushMultisigsSettings(args: Bundle) {
        navigationBuilder().action(R.id.action_pushSettings_to_multisigSettings)
            .setArgs(args)
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
