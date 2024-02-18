package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import android.app.NotificationManager
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.*
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
    resourceManager,
    R.string.default_notification_channel_id,
    R.string.default_notification_channel_name,
) {

    override fun handleNotificationInternal(notificationManager: NotificationManagerCompat, channelId: String, message: RemoteMessage): Boolean {
        val notificationPart = message.notification ?: return false

        val notification = Builder(context, channelId)
            .setContentTitle(notificationPart.title)
            .setContentText(notificationPart.body)
            .setSmallIcon(R.drawable.ic_nova)
            .setPriority(PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)

        return true
    }
}
