package io.novafoundation.nova.feature_push_notifications.presentation.handling

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import io.novafoundation.nova.common.utils.asGsonParsedNumber
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatusType
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.runtime.ext.chainIdHexPrefix16
import io.novafoundation.nova.runtime.ext.onChainAssetId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import java.math.BigInteger

private const val PEDDING_INTENT_REQUEST_CODE = 1

interface PushChainRegestryHolder {

    val chainRegistry: ChainRegistry

    suspend fun NotificationData.getChain(): Chain {
        val chainId = chainId ?: throw NullPointerException("Chain id is null")
        return chainRegistry.chainsById()
            .mapKeys { it.key.chainIdHexPrefix16() }
            .getValue(chainId)
    }
}

internal fun NotificationData.requireType(type: String) {
    require(this.type == type)
}

/**
 * Example: {a_field: {b_field: {c_field: "value"}}}
 * To take a value from c_field use getPayloadFieldContent("a_field", "b_field", "c_field")
 */
internal inline fun <reified T> NotificationData.extractPayloadFieldsWithPath(vararg fields: String): T {
    val fieldsBeforeLast = fields.dropLast(1)
    val last = fields.last()

    val lastSearchingValue = fieldsBeforeLast.fold(payload) { acc, field ->
        acc[field] as? Map<String, Any> ?: throw NullPointerException("Notification parameter $field is null")
    }

    val result = lastSearchingValue[last] ?: return null as T

    return result as? T ?: throw NullPointerException("Notification parameter $last is null")
}

internal fun NotificationData.extractBigInteger(vararg fields: String): BigInteger {
    return extractPayloadFieldsWithPath<Any>(*fields)
        .asGsonParsedNumber()
}

internal fun MetaAccount.formattedAccountName(): String {
    return "[$name]"
}

fun Context.makePendingIntent(intent: Intent): PendingIntent {
    return PendingIntent.getActivity(
        this,
        PEDDING_INTENT_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

fun NotificationCompat.Builder.buildWithDefaults(
    context: Context,
    title: CharSequence,
    message: CharSequence,
    contentIntent: Intent
): NotificationCompat.Builder {
    return setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_nova)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(message)
        )
        .setContentIntent(context.makePendingIntent(contentIntent))
}

fun makeNewReleasesIntent(
    storeLink: String
): Intent {
    return Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(storeLink) }
}

fun ReferendumStatusType.Companion.fromRemoteNotificationType(type: String): ReferendumStatusType {
    return when (type) {
        "Created" -> ReferendumStatusType.PREPARING
        "Deciding" -> ReferendumStatusType.DECIDING
        "Confirming" -> ReferendumStatusType.CONFIRMING
        "Approved" -> ReferendumStatusType.APPROVED
        "Rejected" -> ReferendumStatusType.REJECTED
        "TimedOut" -> ReferendumStatusType.TIMED_OUT
        "Cancelled" -> ReferendumStatusType.CANCELLED
        "Killed" -> ReferendumStatusType.KILLED
        else -> throw IllegalArgumentException("Unknown referendum status type: $this")
    }
}

fun Chain.assetByOnChainAssetIdOrUtility(assetId: String?): Chain.Asset? {
    if (assetId == null) return utilityAsset

    return assets.firstOrNull { it.onChainAssetId == assetId }
}

fun notificationAmountFormat(asset: Chain.Asset, token: Token?, amount: BigInteger): String {
    val tokenAmount = amount.formatPlanks(asset)
    val fiatAmount = token?.planksToFiat(amount)
        ?.formatAsCurrency(token.currency)

    return if (fiatAmount != null) {
        "$tokenAmount ($fiatAmount)"
    } else {
        tokenAmount
    }
}

suspend fun AccountRepository.isNotSingleMetaAccount(): Boolean {
    return getActiveMetaAccountsQuantity() > 1
}
