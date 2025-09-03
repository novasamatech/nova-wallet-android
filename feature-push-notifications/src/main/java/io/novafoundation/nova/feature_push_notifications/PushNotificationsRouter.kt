package io.novafoundation.nova.feature_push_notifications

import android.os.Bundle
import io.novafoundation.nova.common.navigation.ReturnableRouter

interface PushNotificationsRouter : ReturnableRouter {

    fun openPushSettingsWithAccounts()

    fun openPushMultisigsSettings(args: Bundle)

    fun openPushGovernanceSettings(args: Bundle)

    fun openPushStakingSettings(args: Bundle)
}
