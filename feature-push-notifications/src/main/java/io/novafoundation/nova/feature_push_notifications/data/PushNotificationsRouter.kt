package io.novafoundation.nova.feature_push_notifications.data

import io.novafoundation.nova.common.navigation.ReturnableRouter

interface PushNotificationsRouter : ReturnableRouter {
    fun openPushSettings()
}
