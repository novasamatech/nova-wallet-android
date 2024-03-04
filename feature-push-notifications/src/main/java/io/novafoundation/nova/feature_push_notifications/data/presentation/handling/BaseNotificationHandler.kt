package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.fromJson

abstract class BaseNotificationHandler(
    private val notificationIdProvider: NotificationIdProvider,
    private val gson: Gson,
    private val notificationManager: NotificationManagerCompat,
    val resourceManager: ResourceManager,
    private val channel: NovaNotificationChannel,
    private val importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
) : NotificationHandler {

    final override suspend fun handleNotification(message: RemoteMessage): Boolean {
        val channelId = resourceManager.getString(channel.idRes)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                resourceManager.getString(channel.nameRes),
                importance
            )
            notificationManager.createNotificationChannel(channel)
        }

        return runCatching { handleNotificationInternal(channelId, message) }
            .onFailure { it.printStackTrace() }
            .getOrNull() ?: false
    }

    override fun notify(notification: Notification) {
        notificationManager.notify(notificationIdProvider.getId(), notification)
    }

    protected abstract suspend fun handleNotificationInternal(channelId: String, message: RemoteMessage): Boolean

    internal fun RemoteMessage.getMessageContent(): NotificationData {
        val payload: Map<String, Any> = data["payload"]?.let { payload -> gson.fromJson(payload) } ?: emptyMap()

        return NotificationData(
            type = data.getValue("type"),
            chainId = data["chainId"],
            payload = payload
        )
    }
}
