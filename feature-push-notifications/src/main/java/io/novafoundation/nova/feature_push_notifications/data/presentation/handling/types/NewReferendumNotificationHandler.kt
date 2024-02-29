package io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ReferendumDeepLinkConfigPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkConfigurator
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.data.NotificationTypes
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.BaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.DEFAULT_NOTIFICATION_ID
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.PushChainRegestryHolder
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.extractBigInteger
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.makePeddingIntent
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.makeReferendumPendingIntent
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.requireType
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class NewReferendumNotificationHandler(
    private val context: Context,
    private val referendumDeepLinkConfigurator: DeepLinkConfigurator<ReferendumDeepLinkConfigPayload>,
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
        val referendumId = content.extractBigInteger("referendumId")

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(resourceManager.getString(R.string.push_new_referendum_title))
            .setContentText(resourceManager.getString(R.string.push_new_referendum_message, chain.name, referendumId.format()))
            .setSmallIcon(R.drawable.ic_nova)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(context.makeReferendumPendingIntent(referendumDeepLinkConfigurator, chain.id, referendumId))
            .build()

        notificationManager.notify(DEFAULT_NOTIFICATION_ID, notification)

        return true
    }
}
