package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_push_notifications.R

class SendingNotificationHandler(
    private val context: Context,
    notificationManager: NotificationManagerCompat,
    resourceManager: ResourceManager,
) : BaseNotificationHandler(
    notificationManager,
    resourceManager
) {

    override fun handleNotificationInternal(notificationManager: NotificationManagerCompat, channelId: String, message: RemoteMessage): Boolean {
        // TODO check if the notification has a send type

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("\uD83D\uDCB8 Sent [Account name/id]") // TODO stub
            .setContentText("Sent 0.5 ETH (\$900) to 0xc6c...81cdd on Moonbeam") // TODO stub
            .setSmallIcon(R.drawable.ic_nova)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(DEFAULT_NOTIFICATION_ID, notification)

        return true
    }
}
