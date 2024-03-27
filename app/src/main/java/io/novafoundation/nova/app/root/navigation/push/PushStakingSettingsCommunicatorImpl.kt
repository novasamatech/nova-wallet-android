package io.novafoundation.nova.app.root.navigation.push

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.presentation.staking.PushStakingSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.staking.PushStakingSettingsFragment
import io.novafoundation.nova.feature_push_notifications.presentation.staking.PushStakingSettingsRequester
import io.novafoundation.nova.feature_push_notifications.presentation.staking.PushStakingSettingsResponder

class PushStakingSettingsCommunicatorImpl(private val router: PushNotificationsRouter, navigationHolder: NavigationHolder) :
    NavStackInterScreenCommunicator<PushStakingSettingsRequester.Request, PushStakingSettingsResponder.Response>(navigationHolder),
    PushStakingSettingsCommunicator {

    override fun openRequest(request: PushStakingSettingsRequester.Request) {
        super.openRequest(request)

        router.openPushStakingSettings(PushStakingSettingsFragment.getBundle(request))
    }
}
