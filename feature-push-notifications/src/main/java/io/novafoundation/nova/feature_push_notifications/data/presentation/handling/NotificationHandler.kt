package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import android.app.Notification
import com.google.firebase.messaging.RemoteMessage

public const val DEFAULT_NOTIFICATION_ID = 1

interface NotificationHandler {

    /**
     * @return true if the notification was handled, false otherwise
     */
    suspend fun handleNotification(message: RemoteMessage): Boolean

    fun notify(notification: Notification)
}
