package io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_push_notifications.BuildConfig
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.NotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.buildWithDefaults

private const val DEBUG_NOTIFICATION_ID = -1

/**
 * A [NotificationHandler] that is used as a fallback if previous handlers didn't handle the notification
 */
class DebugNotificationHandler(
    private val context: Context,
    private val notificationManager: NotificationManagerCompat,
    private val resourceManager: ResourceManager
) : NotificationHandler {

    override suspend fun handleNotification(message: RemoteMessage): Boolean {
        if (!BuildConfig.DEBUG) return false

        val channelId = resourceManager.getString(R.string.default_notification_channel_id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                resourceManager.getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .buildWithDefaults(
                context,
                "Notification handling error!",
                "The notification was not handled\n${message.data}",
            ).build()

        notify(notification)

        return true
    }

    override fun notify(notification: Notification) {
        notificationManager.notify(DEBUG_NOTIFICATION_ID, notification)
    }
}
