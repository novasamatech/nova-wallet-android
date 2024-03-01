package io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.data.NotificationTypes
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.BaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.DEFAULT_NOTIFICATION_ID
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.NotificationIdReceiver
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.buildWithDefaults
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.extractPayloadField
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.makeReferendumPendingIntent
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.requireType

class NewReleaseNotificationHandler(
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
        val content = message.getMessageContent()
        content.requireType(NotificationTypes.APP_NEW_RELEASE)
        val version = content.extractPayloadField<String>("version")

        val notification = NotificationCompat.Builder(context, channelId)
            .buildWithDefaults(
                context,
                resourceManager.getString(R.string.push_new_update_title),
                resourceManager.getString(R.string.push_new_update_message, version)
            )
            .build()

        notify(notification)

        return true
    }
}
