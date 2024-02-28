package io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.BaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.DEFAULT_NOTIFICATION_ID

class SystemNotificationHandler(
    private val context: Context,
    gson: Gson,
    notificationManager: NotificationManagerCompat,
    resourceManager: ResourceManager,
) : BaseNotificationHandler(
    gson,
    notificationManager,
    resourceManager
) {

    override suspend fun handleNotificationInternal(channelId: String, message: RemoteMessage): Boolean {
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
