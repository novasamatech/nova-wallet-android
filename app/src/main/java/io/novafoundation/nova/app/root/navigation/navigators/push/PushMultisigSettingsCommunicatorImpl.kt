package io.novafoundation.nova.app.root.navigation.navigators.push

import io.novafoundation.nova.app.root.navigation.NavStackInterScreenCommunicator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsFragment
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsRequester
import io.novafoundation.nova.feature_push_notifications.presentation.multisigs.PushMultisigSettingsResponder

class PushMultisigSettingsCommunicatorImpl(private val router: PushNotificationsRouter, navigationHoldersRegistry: NavigationHoldersRegistry) :
    NavStackInterScreenCommunicator<PushMultisigSettingsRequester.Request, PushMultisigSettingsResponder.Response>(navigationHoldersRegistry),
    PushMultisigSettingsCommunicator {

    override fun openRequest(request: PushMultisigSettingsRequester.Request) {
        super.openRequest(request)

        router.openPushGovernanceSettings(PushMultisigSettingsFragment.createPayload(request))
    }
}
