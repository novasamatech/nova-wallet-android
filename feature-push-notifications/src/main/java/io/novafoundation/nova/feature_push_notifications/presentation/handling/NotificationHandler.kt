package io.novafoundation.nova.feature_push_notifications.presentation.handling

import com.google.firebase.messaging.RemoteMessage

interface NotificationHandler {

    /**
     * @return true if the notification was handled, false otherwise
     */
    suspend fun handleNotification(message: RemoteMessage): Boolean
}
