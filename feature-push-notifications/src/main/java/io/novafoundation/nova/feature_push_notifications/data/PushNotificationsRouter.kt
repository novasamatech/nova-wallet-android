package io.novafoundation.nova.feature_push_notifications.data

import android.os.Bundle
import io.novafoundation.nova.common.navigation.ReturnableRouter

interface PushNotificationsRouter : ReturnableRouter {

    fun openPushSettings()

    fun openPushGovernanceSettings(args: Bundle)

    fun openPushStakingSettings(args: Bundle)
}
