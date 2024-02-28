package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.runtime.ext.chainIdHexPrefix16
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainsById

abstract class BaseNotificationHandler(
    private val gson: Gson,
    protected val notificationManager: NotificationManagerCompat,
    val resourceManager: ResourceManager,
    @StringRes private val channelIdRes: Int = R.string.default_notification_channel_id,
    @StringRes private val channelNameRes: Int = R.string.default_notification_channel_name,
    private val importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
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

        return runCatching { handleNotificationInternal(channelId, message) }
            .getOrNull() ?: false
    }

    protected abstract suspend fun handleNotificationInternal(channelId: String, message: RemoteMessage): Boolean

    internal fun RemoteMessage.getMessageContent(): MessageContent {
        val messageValue = data.getValue("message")
        return runCatching { gson.fromJson<MessageContent>(messageValue) }
            .getOrThrow()
    }
}
