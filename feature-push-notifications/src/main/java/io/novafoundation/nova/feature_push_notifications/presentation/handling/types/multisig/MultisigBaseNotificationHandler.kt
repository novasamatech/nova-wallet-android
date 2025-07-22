package io.novafoundation.nova.feature_push_notifications.presentation.handling.types.multisig

import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.presentation.handling.BaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NotificationIdProvider
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NovaNotificationChannel
import io.novafoundation.nova.feature_push_notifications.presentation.handling.PushChainRegestryHolder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

abstract class MultisigBaseNotificationHandler(
    private val multisigCallFormatter: MultisigCallFormatter,
    override val chainRegistry: ChainRegistry,
    activityIntentProvider: ActivityIntentProvider,
    notificationIdProvider: NotificationIdProvider,
    gson: Gson,
    notificationManager: NotificationManagerCompat,
    resourceManager: ResourceManager
) : BaseNotificationHandler(
    activityIntentProvider,
    notificationIdProvider,
    gson,
    notificationManager,
    resourceManager,
    channel = NovaNotificationChannel.MULTISIG
),
    PushChainRegestryHolder {

    fun getSubText(metaAccount: MetaAccount): String {
        return resourceManager.getString(R.string.multisig_notification_message_header, metaAccount.name)
    }

    suspend fun getMessage(
        chain: Chain,
        payload: MultisigNotificationPayload,
        additionalMessage: String? = null,
        footer: String? = null
    ): String {
        val runtime = chainRegistry.getRuntime(chain.id)
        val call = payload.callData?.let { GenericCall.fromHex(runtime, payload.callData) }

        return buildString {
            append(multisigCallFormatter.formatPushNotificationMessage(call, payload.signatory.accountId, chain))
            if (additionalMessage != null) {
                appendLine()
                append(additionalMessage)
            }
            if (footer != null) {
                appendLine()
                append(footer)
            }
        }
    }
}
