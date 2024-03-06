package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import com.google.firebase.messaging.RemoteMessage

class CompoundNotificationHandler(
    val handlers: Set<NotificationHandler>
) : NotificationHandler {

    override suspend fun handleNotification(message: RemoteMessage): Boolean {
        for (handler in handlers) {
            if (handler.handleNotification(message)) {
                return true
            }
        }

        return false
    }
}
