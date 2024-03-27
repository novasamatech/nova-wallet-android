package io.novafoundation.nova.feature_push_notifications.presentation.handling.types

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ReferendumDeepLinkData
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkConfigurator
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatusType
import io.novafoundation.nova.feature_governance_api.presentation.referenda.common.ReferendaStatusFormatter
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.NotificationTypes
import io.novafoundation.nova.feature_push_notifications.presentation.handling.BaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NotificationIdProvider
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NovaNotificationChannel
import io.novafoundation.nova.feature_push_notifications.presentation.handling.PushChainRegestryHolder
import io.novafoundation.nova.feature_push_notifications.presentation.handling.addReferendumData
import io.novafoundation.nova.feature_push_notifications.presentation.handling.buildWithDefaults
import io.novafoundation.nova.feature_push_notifications.presentation.handling.extractBigInteger
import io.novafoundation.nova.feature_push_notifications.presentation.handling.extractPayloadFieldsWithPath
import io.novafoundation.nova.feature_push_notifications.presentation.handling.fromRemoteNotificationType
import io.novafoundation.nova.feature_push_notifications.presentation.handling.requireType
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class ReferendumStateUpdateNotificationHandler(
    private val context: Context,
    private val configurator: DeepLinkConfigurator<ReferendumDeepLinkData>,
    private val referendaStatusFormatter: ReferendaStatusFormatter,
    override val chainRegistry: ChainRegistry,
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
    channel = NovaNotificationChannel.GOVERNANCE
),
    PushChainRegestryHolder {

    override suspend fun handleNotificationInternal(channelId: String, message: RemoteMessage): Boolean {
        val content = message.getMessageContent()
        content.requireType(NotificationTypes.GOV_STATE)
        val chain = content.getChain()
        val referendumId = content.extractBigInteger("referendumId")
        val stateFrom = content.extractPayloadFieldsWithPath<String?>("from")?.let { ReferendumStatusType.fromRemoteNotificationType(it) }
        val stateTo = content.extractPayloadFieldsWithPath<String>("to").let { ReferendumStatusType.fromRemoteNotificationType(it) }

        val notification = NotificationCompat.Builder(context, channelId)
            .buildWithDefaults(
                context,
                getTitle(stateTo),
                getMessage(chain, referendumId, stateFrom, stateTo),
                activityIntent().addReferendumData(configurator, chain.id, referendumId)
            ).build()

        notify(notification)

        return true
    }

    private fun getTitle(refStateTo: ReferendumStatusType): String {
        return when (refStateTo) {
            ReferendumStatusType.APPROVED -> resourceManager.getString(R.string.push_referendum_approved_title)
            ReferendumStatusType.REJECTED -> resourceManager.getString(R.string.push_referendum_rejected_title)
            else -> resourceManager.getString(R.string.push_referendum_status_changed_title)
        }
    }

    private fun getMessage(chain: Chain, referendumId: BigInteger, stateFrom: ReferendumStatusType?, stateTo: ReferendumStatusType): String {
        return when {
            stateTo == ReferendumStatusType.APPROVED -> resourceManager.getString(R.string.push_referendum_approved_message, chain.name, referendumId.format())
            stateTo == ReferendumStatusType.REJECTED -> resourceManager.getString(R.string.push_referendum_rejected_message, chain.name, referendumId.format())
            stateFrom == null -> resourceManager.getString(
                R.string.push_referendum_to_status_changed_message,
                chain.name,
                referendumId.format(),
                referendaStatusFormatter.formatStatus(stateTo)
            )

            else -> resourceManager.getString(
                R.string.push_referendum_from_to_status_changed_message,
                chain.name,
                referendumId.format(),
                referendaStatusFormatter.formatStatus(stateFrom),
                referendaStatusFormatter.formatStatus(stateTo)
            )
        }
    }
}
