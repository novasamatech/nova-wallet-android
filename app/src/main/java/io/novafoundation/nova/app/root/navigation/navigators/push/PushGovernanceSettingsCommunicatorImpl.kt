package io.novafoundation.nova.app.root.navigation.navigators.push

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsFragment
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsRequester
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsResponder

class PushGovernanceSettingsCommunicatorImpl(private val router: PushNotificationsRouter, navigationHolder: SplitScreenNavigationHolder) :
    NavStackInterScreenCommunicator<PushGovernanceSettingsRequester.Request, PushGovernanceSettingsResponder.Response>(navigationHolder),
    PushGovernanceSettingsCommunicator {

    override fun openRequest(request: PushGovernanceSettingsRequester.Request) {
        super.openRequest(request)

        router.openPushGovernanceSettings(PushGovernanceSettingsFragment.getBundle(request))
    }
}
