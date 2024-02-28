package io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.data.NotificationTypes
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.BaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.DEFAULT_NOTIFICATION_ID
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.PushChainRegestryHolder
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.extractAmount
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.extractPayloadField
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.formattedAccountName
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.requireType
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.onChainAssetId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class TokenReceivedNotificationHandler(
    private val context: Context,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
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
        content.requireType(NotificationTypes.TOKENS_RECEIVED)
        val chain = content.getChain()
        val sender = content.extractPayloadField<String>("sender")
        val recepient = content.extractPayloadField<String>("recepient")
        val assetId = content.extractPayloadField<String?>("assetId")
        val amount = content.extractAmount("amount")

        val metaAccountsQuantity = accountRepository.getActiveMetaAccountsQuantity()
        val senderMetaAccount = accountRepository.findMetaAccount(chain.accountIdOf(sender), chain.id)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(getTitle(metaAccountsQuantity, senderMetaAccount))
            .setContentText(getMessage(chain, assetId, amount))
            .setSmallIcon(R.drawable.ic_nova)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(DEFAULT_NOTIFICATION_ID, notification)

        return true
    }

    private fun getTitle(metaAccountsQuantity: Int, senderMetaAccount: MetaAccount?): String {
        val accountName = senderMetaAccount?.formattedAccountName()
        return when {
            metaAccountsQuantity > 1 && accountName != null -> resourceManager.getString(R.string.push_token_received_title, accountName)
            else -> resourceManager.getString(R.string.push_token_received_no_account_name_title)
        }
    }

    private suspend fun getMessage(
        chain: Chain,
        assetId: String?,
        amount: BigInteger
    ): String {
        val asset = chain.assets.firstOrNull { it.onChainAssetId == assetId } ?: chain.utilityAsset
        val token = tokenRepository.getTokenOrNull(asset)
        val tokenAmount = amount.formatPlanks(asset)
        val fiatAmount = token?.planksToFiat(amount)
            ?.formatAsCurrency(token.currency)

        return when {
            fiatAmount != null -> resourceManager.getString(R.string.push_token_sent_message, tokenAmount, fiatAmount, chain.name)
            else -> resourceManager.getString(R.string.push_token_sent_message_no_fiat, tokenAmount, chain.name)
        }
    }
}
