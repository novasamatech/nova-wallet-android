package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_push_notifications.R

class DefaultNotificationHandler(
    private val context: Context,
    notificationManager: NotificationManagerCompat,
    resourceManager: ResourceManager,
) : BaseNotificationHandler(
    notificationManager,
    resourceManager
) {

    override fun handleNotificationInternal(notificationManager: NotificationManagerCompat, channelId: String, message: RemoteMessage): Boolean {
        val notificationPart = message.notification ?: return false

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(notificationPart.title)
            .setContentText(notificationPart.body)
            .setSmallIcon(R.drawable.ic_nova)
            .setPriority(PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(DEFAULT_NOTIFICATION_ID, notification)

        return true
    }
}
