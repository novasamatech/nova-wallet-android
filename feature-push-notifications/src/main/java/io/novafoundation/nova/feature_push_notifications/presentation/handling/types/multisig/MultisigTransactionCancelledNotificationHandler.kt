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
import io.novafoundation.nova.feature_account_api.domain.account.identity.getNameOrAddress
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.applyDeepLink
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkConfigurator
import io.novafoundation.nova.feature_multisig_operations.presentation.details.deeplink.MultisigOperationDeepLinkData
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.NotificationTypes
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NotificationIdProvider
import io.novafoundation.nova.feature_push_notifications.presentation.handling.PushChainRegestryHolder
import io.novafoundation.nova.feature_push_notifications.presentation.handling.buildWithDefaults
import io.novafoundation.nova.feature_push_notifications.presentation.handling.requireType
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class MultisigTransactionCancelledNotificationHandler(
    private val context: Context,
    private val accountRepository: AccountRepository,
    private val multisigCallFormatter: MultisigCallFormatter,
    private val configurator: MultisigOperationDeepLinkConfigurator,
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
        content.requireType(NotificationTypes.MULTISIG_CANCELLED)

        val chain = content.getChain()
        require(chain.isEnabled)

        val payload = content.extractMultisigPayload(signatoryRole = "canceller", chain)

        val multisigAccount = accountRepository.getMultisigForPayload(chain, payload) ?: return true

        val rejecterIdentity = identityProvider.getNameOrAddress(payload.signatory.accountId, chain)
        val messageText = getMessage(
            chain,
            payload,
            footer = resourceManager.getString(R.string.multisig_notification_rejected_transaction_message, rejecterIdentity)
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSubText(getSubText(multisigAccount))
            .buildWithDefaults(
                context,
                resourceManager.getString(R.string.multisig_notification_rejected_transaction_title),
                messageText,
                activityIntent().applyDeepLink(
                    configurator,
                    multisigOperationDeepLinkData(multisigAccount, chain, payload, MultisigOperationDeepLinkData.State.Rejected(rejecterIdentity))
                )
            ).build()

        notify(notification)

        return true
    }
}
