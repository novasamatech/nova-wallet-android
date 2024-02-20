package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_push_notifications.R

abstract class BaseNotificationHandler(
    private val notificationManager: NotificationManagerCompat,
    val resourceManager: ResourceManager,
    @StringRes private val channelIdRes: Int = R.string.default_notification_channel_id,
    @StringRes private val channelNameRes: Int = R.string.default_notification_channel_name,
    private val importance: Int = NotificationManager.IMPORTANCE_DEFAULT
) : NotificationHandler {

    final override suspend fun handleNotification(message: RemoteMessage): Boolean {
        val channelId = resourceManager.getString(channelIdRes)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                resourceManager.getString(channelNameRes),
                importance
            )
            notificationManager.createNotificationChannel(channel)
        }

        return handleNotificationInternal(notificationManager, channelId, message)
    }

    protected abstract fun handleNotificationInternal(notificationManager: NotificationManagerCompat, channelId: String, message: RemoteMessage): Boolean
}
