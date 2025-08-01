package io.novafoundation.nova.feature_push_notifications.presentation.handling.types.multisig

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalWithOnChainIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.NotificationTypes
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NotificationIdProvider
import io.novafoundation.nova.feature_push_notifications.presentation.handling.PushChainRegestryHolder
import io.novafoundation.nova.feature_push_notifications.presentation.handling.buildWithDefaults
import io.novafoundation.nova.feature_push_notifications.presentation.handling.requireType
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class MultisigTransactionExecutedNotificationHandler(
    private val context: Context,
    private val accountRepository: AccountRepository,
    private val multisigCallFormatter: MultisigCallFormatter,
    @LocalWithOnChainIdentity private val identityProvider: IdentityProvider,
    override val chainRegistry: ChainRegistry,
    activityIntentProvider: ActivityIntentProvider,
    notificationIdProvider: NotificationIdProvider,
    gson: Gson,
    notificationManager: NotificationManagerCompat,
    resourceManager: ResourceManager,
) : MultisigBaseNotificationHandler(
    multisigCallFormatter,
    chainRegistry,
    activityIntentProvider,
    notificationIdProvider,
    gson,
    notificationManager,
    resourceManager
),
    PushChainRegestryHolder {

    override suspend fun handleNotificationInternal(channelId: String, message: RemoteMessage): Boolean {
        val content = message.getMessageContent()
        content.requireType(NotificationTypes.MULTISIG_EXECUTED)

        val chain = content.getChain()
        require(chain.isEnabled)

        val payload = content.extractMultisigPayload(signatoryRole = "approver", chain)

        val multisigAccount = accountRepository.getMultisigForPayload(chain, payload) ?: return true

        val approverIdentity = identityProvider.getNameOrAddress(payload.signatory, chain)
        val messageText = getMessage(
            chain,
            payload,
            additionalMessage = resourceManager.getString(R.string.multisig_notification_executed_transaction_message, approverIdentity)
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSubText(getSubText(multisigAccount))
            .buildWithDefaults(
                context,
                resourceManager.getString(R.string.multisig_notification_executed_transaction_title),
                messageText,
                activityIntent()
            ).build()

        notify(notification)

        return true
    }
}
