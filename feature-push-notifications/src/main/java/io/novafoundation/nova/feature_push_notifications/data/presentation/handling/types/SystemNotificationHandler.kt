package io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.BaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.NotificationIdProvider
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.NovaNotificationChannel
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.buildWithDefaults

class SystemNotificationHandler(
    private val context: Context,
    activityIntentProvider: ActivityIntentProvider,
    notificationIdProvider: NotificationIdProvider,
    gson: Gson,
    notificationManager: NotificationManagerCompat,
    resourceManager: ResourceManager,
) : BaseNotificationHandler(
    activityIntentProvider,
    notificationIdProvider,
    gson,
    notificationManager,
    resourceManager,
    channel = NovaNotificationChannel.DEFAULT
) {

    override suspend fun handleNotificationInternal(channelId: String, message: RemoteMessage): Boolean {
        val notificationPart = message.notification ?: return false

        val title = notificationPart.title ?: return false
        val body = notificationPart.body ?: return false

        val notification = NotificationCompat.Builder(context, channelId)
            .buildWithDefaults(context, title, body, activityIntent())
            .build()

        notify(notification)

        return true
    }
}
