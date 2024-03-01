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
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.NotificationIdReceiver
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.buildWithDefaults
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.makeAssetDetailsPendingIntent
import io.novafoundation.nova.runtime.ext.utilityAsset

class SystemNotificationHandler(
    private val context: Context,
    notificationIdReceiver: NotificationIdReceiver,
    gson: Gson,
    notificationManager: NotificationManagerCompat,
    resourceManager: ResourceManager,
) : BaseNotificationHandler(
    notificationIdReceiver,
    gson,
    notificationManager,
    resourceManager
) {

    override suspend fun handleNotificationInternal(channelId: String, message: RemoteMessage): Boolean {
        val notificationPart = message.notification ?: return false

        val title = notificationPart.title ?: return false
        val body = notificationPart.body ?: return false

        val notification = NotificationCompat.Builder(context, channelId)
            .buildWithDefaults(context, title, body)
            .build()

        notify(notification)

        return true
    }
}
