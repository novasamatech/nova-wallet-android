package io.novafoundation.nova.feature_push_notifications.presentation.handling.types

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
import io.novafoundation.nova.feature_push_notifications.data.NotificationTypes
import io.novafoundation.nova.feature_push_notifications.presentation.handling.BaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NotificationIdProvider
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NovaNotificationChannel
import io.novafoundation.nova.feature_push_notifications.presentation.handling.PushChainRegestryHolder
import io.novafoundation.nova.feature_push_notifications.presentation.handling.addAssetDetailsData
import io.novafoundation.nova.feature_push_notifications.presentation.handling.buildWithDefaults
import io.novafoundation.nova.feature_push_notifications.presentation.handling.extractBigInteger
import io.novafoundation.nova.feature_push_notifications.presentation.handling.extractPayloadFieldsWithPath
import io.novafoundation.nova.feature_push_notifications.presentation.handling.notificationAmountFormat
import io.novafoundation.nova.feature_push_notifications.presentation.handling.formattedAccountName
import io.novafoundation.nova.feature_push_notifications.presentation.handling.isNotSingleMetaAccount
import io.novafoundation.nova.feature_push_notifications.presentation.handling.requireType
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class StakingRewardNotificationHandler(
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
    channel = NovaNotificationChannel.STAKING
),
    PushChainRegestryHolder {

    override suspend fun handleNotificationInternal(channelId: String, message: RemoteMessage): Boolean {
        val content = message.getMessageContent()
        content.requireType(NotificationTypes.STAKING_REWARD)
        val chain = content.getChain()
        val recipient = content.extractPayloadFieldsWithPath<String>("recipient")
        val amount = content.extractBigInteger("amount")

        val metaAccount = accountRepository.findMetaAccount(chain.accountIdOf(recipient), chain.id) ?: return false

        val notification = NotificationCompat.Builder(context, channelId)
            .buildWithDefaults(
                context,
                getTitle(metaAccount),
                getMessage(chain, amount),
                activityIntent().addAssetDetailsData(configurator, recipient, chain.id, chain.utilityAsset.id)
            ).build()

        notify(notification)

        return true
    }

    private suspend fun getTitle(metaAccount: MetaAccount): String {
        return when {
            accountRepository.isNotSingleMetaAccount() -> resourceManager.getString(
                R.string.push_staking_reward_many_accounts_title,
                metaAccount.formattedAccountName()
            )

            else -> resourceManager.getString(R.string.push_staking_reward_single_account_title)
        }
    }

    private suspend fun getMessage(
        chain: Chain,
        amount: BigInteger
    ): String {
        val asset = chain.utilityAsset
        val token = tokenRepository.getTokenOrNull(asset)
        val formattedAmount = notificationAmountFormat(asset, token, amount)

        return resourceManager.getString(R.string.push_staking_reward_message, formattedAmount, chain.name)
    }
}
