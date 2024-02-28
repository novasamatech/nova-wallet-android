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
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.PushChainRegestryHolder
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.extractPayloadField
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.requireType
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class NewReferendumNotificationHandler(
    private val context: Context,
    override val chainRegistry: ChainRegistry,
    gson: Gson,
    notificationManager: NotificationManagerCompat,
    resourceManager: ResourceManager,
) : BaseNotificationHandler(
    gson,
    notificationManager,
    resourceManager
), PushChainRegestryHolder {

    override suspend fun handleNotificationInternal(channelId: String, message: RemoteMessage): Boolean {
        val content = message.getMessageContent()
        content.requireType(NotificationTypes.GOV_NEW_REF)
        val chain = content.getChain()
        val referendumId = content.extractPayloadField<Int>("referendumId")
            .format()

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(resourceManager.getString(R.string.push_new_referendum_title))
            .setContentText(resourceManager.getString(R.string.push_new_referendum_message, chain.name, referendumId))
            .setSmallIcon(R.drawable.ic_nova)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(DEFAULT_NOTIFICATION_ID, notification)

        return true
    }
}
