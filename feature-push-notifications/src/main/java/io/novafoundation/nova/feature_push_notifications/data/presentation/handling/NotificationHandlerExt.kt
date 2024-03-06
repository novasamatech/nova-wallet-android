package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.AssetDetailsLinkConfigPayload
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ReferendumDeepLinkConfigPayload
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkConfigurator
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.runtime.ext.chainIdHexPrefix16
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
internal inline fun <reified T> NotificationData.extractPayloadField(vararg fields: String): T {
    var payloadContent = payload

    val fieldsBeforeLast = fields.dropLast(1)
    val last = fields.last()

    fieldsBeforeLast.forEach {
        payloadContent = payloadContent[it] as? Map<String, Any> ?: throw NullPointerException("Notification parameter $it is null")
    }

    val result = payloadContent[last] ?: return null as T

    return result as? T ?: throw NullPointerException("Notification parameter $last is null")
}

internal fun NotificationData.extractBigInteger(vararg fields: String): BigInteger {
    return when (val value = extractPayloadField<Any>(*fields)) {
        is Float -> value.toLong().toBigInteger()
        is Double -> value.toLong().toBigInteger()
        else -> BigInteger(value.toString())
    }
}

internal fun MetaAccount.formattedAccountName(): String {
    return "[$name]"
}

fun Context.makePendingIntent(intent: Intent): PendingIntent {
    return PendingIntent.getActivity(
        this,
        PEDDING_INTENT_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun makeReferendumPendingIntent(
    deepLinkConfigurator: DeepLinkConfigurator<ReferendumDeepLinkConfigPayload>,
    chainId: String,
    referendumId: BigInteger
): Intent {
    val payload = ReferendumDeepLinkConfigPayload(chainId, referendumId, Chain.Governance.V2)
    val deepLink = deepLinkConfigurator.configure(payload)
    return Intent(Intent.ACTION_VIEW, deepLink)
}

fun makeAssetDetailsPendingIntent(
    deepLinkConfigurator: DeepLinkConfigurator<AssetDetailsLinkConfigPayload>,
    chainId: String,
    assetId: Int
): Intent {
    val payload = AssetDetailsLinkConfigPayload(chainId, assetId)
    val deepLink = deepLinkConfigurator.configure(payload)
    return Intent(Intent.ACTION_VIEW, deepLink)
}

fun NotificationCompat.Builder.buildWithDefaults(
    context: Context,
    title: String,
    message: String,
    contentIntent: Intent
): NotificationCompat.Builder {
    return setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_nova)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(message)
        )
        .setContentIntent(context.makePendingIntent(contentIntent))
}
