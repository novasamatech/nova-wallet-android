package io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.AssetDetailsLinkConfigPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkConfigurator
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.data.NotificationTypes
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.BaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.DEFAULT_NOTIFICATION_ID
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.PushChainRegestryHolder
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.extractBigInteger
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.extractPayloadField
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.formattedAccountName
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.makeAssetDetailsPendingIntent
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.requireType
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class StakingRewardNotificationHandler(
    private val context: Context,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val deepLinkConfigurator: DeepLinkConfigurator<AssetDetailsLinkConfigPayload>,
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
        content.requireType(NotificationTypes.TOKENS_SENT)
        val chain = content.getChain()
        val recepient = content.extractPayloadField<String>("recipient")
        val amount = content.extractBigInteger("amount")

        val metaAccountsCount = accountRepository.getActiveMetaAccountsQuantity()
        val metaAccount = accountRepository.findMetaAccount(chain.accountIdOf(recepient), chain.id) ?: return false

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(getTitle(metaAccountsCount, metaAccount))
            .setContentText(getMessage(chain, amount))
            .setSmallIcon(R.drawable.ic_nova)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(context.makeAssetDetailsPendingIntent(deepLinkConfigurator, chain.id, chain.utilityAsset.id))
            .build()

        notificationManager.notify(
            DEFAULT_NOTIFICATION_ID, notification
        )

        return true
    }

    private fun getTitle(metaAccountsQuantity: Int, metaAccount: MetaAccount): String {
        return when {
            metaAccountsQuantity > 1 -> resourceManager.getString(R.string.push_staking_reward_many_accounts_title, metaAccount.formattedAccountName())
            else -> resourceManager.getString(R.string.push_staking_reward_single_account_title)
        }
    }

    private suspend fun getMessage(
        chain: Chain,
        amount: BigInteger
    ): String {
        val asset = chain.utilityAsset
        val token = tokenRepository.getTokenOrNull(asset)
        val tokenAmount = amount.formatPlanks(asset)
        val fiatAmount = token?.planksToFiat(amount)
            ?.formatAsCurrency(token.currency)
        return when {
            fiatAmount == null -> resourceManager.getString(R.string.push_staking_reward_message_no_fiat, tokenAmount, chain.name)
            else -> resourceManager.getString(R.string.push_staking_reward_message_no_fiat, tokenAmount, fiatAmount, chain.name)
        }
    }
}