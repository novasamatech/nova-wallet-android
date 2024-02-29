package io.novafoundation.nova.feature_push_notifications.data.presentation.handling

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.AssetDetailsDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.AssetDetailsLinkConfigPayload
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ReferendumDeepLinkConfigPayload
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkConfigurator
import io.novafoundation.nova.runtime.ext.chainIdHexPrefix16
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import java.math.BigInteger

private const val PEDDING_INTENT_REQUEST_CODE = 1

interface PushChainRegestryHolder {

    val chainRegistry: ChainRegistry

    suspend fun MessageContent.getChain(): Chain {
        val chainId = data?.chainId ?: throw NullPointerException("Chain id is null")
        return chainRegistry.chainsById()
            .mapKeys { it.key.chainIdHexPrefix16() }
            .getValue(chainId)
    }
}

internal fun MessageContent.requireType(type: String) {
    require(data?.type == type)
}

/**
 * Example: {a_field: {b_field: {c_field: "value"}}}
 * To take a value from c_field use getPayloadFieldContent("a_field", "b_field", "c_field")
 */
internal inline fun <reified T> MessageContent.extractPayloadField(vararg fields: String): T {
    var payloadContent = data?.payload ?: throw NullPointerException("Payload is null")

    val content = fields.dropLast(1)
    val last = fields.last()

    content.forEach {
        payloadContent = payloadContent[it] as? Map<String, Any> ?: throw NullPointerException("Payload is null")
    }

    return payloadContent[last] as? T ?: throw NullPointerException("Payload is null")
}

internal fun MessageContent.extractBigInteger(vararg fields: String): BigInteger {
    val amount = extractPayloadField<String>(*fields)
    return BigInteger(amount)
}

internal fun MetaAccount.formattedAccountName(): String {
    return "[$name]"
}

fun Context.makePeddingIntent(intent: Intent): PendingIntent {
    return PendingIntent.getActivity(
        this,
        PEDDING_INTENT_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun Context.makeReferendumPendingIntent(
    deepLinkConfigurator: DeepLinkConfigurator<ReferendumDeepLinkConfigPayload>,
    chainId: String,
    referendumId: BigInteger
): PendingIntent {
    val payload = ReferendumDeepLinkConfigPayload(chainId, referendumId, Chain.Governance.V2)
    val deepLink = deepLinkConfigurator.configure(payload)
    return makePeddingIntent(Intent(Intent.ACTION_VIEW, deepLink))
}


fun Context.makeAssetDetailsPendingIntent(
    deepLinkConfigurator: DeepLinkConfigurator<AssetDetailsLinkConfigPayload>,
    chainId: String,
    assetId: Int
): PendingIntent {
    val payload = AssetDetailsLinkConfigPayload(chainId, assetId)
    val deepLink = deepLinkConfigurator.configure(payload)
    return makePeddingIntent(Intent(Intent.ACTION_VIEW, deepLink))
}
