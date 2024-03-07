package io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.AssetDetailsDeepLinkData
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkConfigurator
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.data.data.NotificationTypes
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.BaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.NotificationIdProvider
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.NovaNotificationChannel
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.PushChainRegestryHolder
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.addAssetDetailsData
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.assetByOnChainAssetIdOrUtility
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.buildWithDefaults
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.extractBigInteger
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.extractPayloadFieldsWithPath
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.formattedAccountName
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.isNotSingleMetaAccount
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.notificationAmountFormat
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.requireType
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class TokenReceivedNotificationHandler(
    private val context: Context,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val configurator: DeepLinkConfigurator<AssetDetailsDeepLinkData>,
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
    channel = NovaNotificationChannel.TRANSACTIONS
),
    PushChainRegestryHolder {

    override suspend fun handleNotificationInternal(channelId: String, message: RemoteMessage): Boolean {
        val content = message.getMessageContent()
        content.requireType(NotificationTypes.TOKENS_RECEIVED)
        val chain = content.getChain()
        val recipient = content.extractPayloadFieldsWithPath<String>("recipient")
        val assetId = content.extractPayloadFieldsWithPath<String?>("assetId")
        val amount = content.extractBigInteger("amount")

        val asset = chain.assetByOnChainAssetIdOrUtility(assetId) ?: return false
        val recipientMetaAccount = accountRepository.findMetaAccount(chain.accountIdOf(recipient), chain.id) ?: return false

        val notification = NotificationCompat.Builder(context, channelId)
            .buildWithDefaults(
                context,
                getTitle(recipientMetaAccount),
                getMessage(chain, asset, amount),
                activityIntent().addAssetDetailsData(configurator, chain.id, asset.id)
            ).build()

        notify(notification)

        return true
    }

    private suspend fun getTitle(senderMetaAccount: MetaAccount?): String {
        val accountName = senderMetaAccount?.formattedAccountName()
        return when {
            accountRepository.isNotSingleMetaAccount() && accountName != null -> resourceManager.getString(R.string.push_token_received_title, accountName)
            else -> resourceManager.getString(R.string.push_token_received_no_account_name_title)
        }
    }

    private suspend fun getMessage(
        chain: Chain,
        asset: Chain.Asset,
        amount: BigInteger
    ): String {
        val token = tokenRepository.getTokenOrNull(asset)
        val formattedAmount = notificationAmountFormat(asset, token, amount)

        return resourceManager.getString(R.string.push_token_received_message, formattedAmount, chain.name)
    }
}
