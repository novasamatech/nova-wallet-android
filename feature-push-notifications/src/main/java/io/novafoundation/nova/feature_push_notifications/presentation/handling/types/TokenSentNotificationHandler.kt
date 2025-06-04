package io.novafoundation.nova.feature_push_notifications.presentation.handling.types

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkData
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.applyDeepLink
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.NotificationTypes
import io.novafoundation.nova.feature_push_notifications.presentation.handling.BaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NotificationIdProvider
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NovaNotificationChannel
import io.novafoundation.nova.feature_push_notifications.presentation.handling.PushChainRegestryHolder
import io.novafoundation.nova.feature_push_notifications.presentation.handling.assetByOnChainAssetIdOrUtility
import io.novafoundation.nova.feature_push_notifications.presentation.handling.buildWithDefaults
import io.novafoundation.nova.feature_push_notifications.presentation.handling.extractBigInteger
import io.novafoundation.nova.feature_push_notifications.presentation.handling.extractPayloadFieldsWithPath
import io.novafoundation.nova.feature_push_notifications.presentation.handling.formattedAccountName
import io.novafoundation.nova.feature_push_notifications.presentation.handling.isNotSingleMetaAccount
import io.novafoundation.nova.feature_push_notifications.presentation.handling.notificationAmountFormat
import io.novafoundation.nova.feature_push_notifications.presentation.handling.requireType
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class TokenSentNotificationHandler(
    private val context: Context,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    override val chainRegistry: ChainRegistry,
    private val configurator: io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkConfigurator,
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
    channel = NovaNotificationChannel.TRANSACTIONS
),
    PushChainRegestryHolder {

    override suspend fun handleNotificationInternal(channelId: String, message: RemoteMessage): Boolean {
        val content = message.getMessageContent()
        content.requireType(NotificationTypes.TOKENS_SENT)

        val chain = content.getChain()
        require(chain.isEnabled)

        val sender = content.extractPayloadFieldsWithPath<String>("sender")
        val recipient = content.extractPayloadFieldsWithPath<String>("recipient")
        val assetId = content.extractPayloadFieldsWithPath<String?>("assetId")
        val amount = content.extractBigInteger("amount")

        val asset = chain.assetByOnChainAssetIdOrUtility(assetId) ?: return false
        val senderMetaAccount = accountRepository.findMetaAccount(chain.accountIdOf(sender), chain.id) ?: return false
        val recipientMetaAccount = accountRepository.findMetaAccount(chain.accountIdOf(recipient), chain.id)

        val notification = NotificationCompat.Builder(context, channelId)
            .buildWithDefaults(
                context,
                getTitle(senderMetaAccount),
                getMessage(chain, recipientMetaAccount, recipient, asset, amount),
                activityIntent().applyDeepLink(
                    configurator,
                    AssetDetailsDeepLinkData(sender, chain.id, asset.id)
                )
            ).build()

        notify(notification)

        return true
    }

    private suspend fun getTitle(senderMetaAccount: MetaAccount?): String {
        val accountName = senderMetaAccount?.formattedAccountName()
        return when {
            accountRepository.isNotSingleMetaAccount() && accountName != null -> resourceManager.getString(R.string.push_token_sent_title, accountName)
            else -> resourceManager.getString(R.string.push_token_sent_no_account_name_title)
        }
    }

    private suspend fun getMessage(
        chain: Chain,
        recipientMetaAccount: MetaAccount?,
        recipientAddress: String,
        asset: Chain.Asset,
        amount: BigInteger
    ): String {
        val token = tokenRepository.getTokenOrNull(asset)
        val formattedAmount = notificationAmountFormat(asset, token, amount)

        val accountNameOrAddress = recipientMetaAccount?.formattedAccountName() ?: recipientAddress

        return resourceManager.getString(R.string.push_token_sent_message, formattedAmount, accountNameOrAddress, chain.name)
    }
}
