package io.novafoundation.nova.feature_gift_impl.presentation.claim.deeplink

import android.net.Uri
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.DeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.addParamIfNotNull
import io.novafoundation.nova.runtime.ext.ChainGeneses
import kotlin.math.min

class ClaimGiftDeepLinkData(
    val seed: String,
    val chainId: String,
    val symbol: TokenSymbol
)

class ClaimGiftPayloadParams(
    val seed: String,
    val chainIdPrefix: String?,
    val symbol: TokenSymbol?
)

class ClaimGiftDeepLinkConfigurator(
    private val linkBuilderFactory: LinkBuilderFactory
) : DeepLinkConfigurator<ClaimGiftDeepLinkData> {

    val action = "open"
    val screen = "gift"
    val deepLinkPrefix = "/$action/$screen"
    val payloadParam = "payload"
    val chainIdLength = 6

    private val defaultChainId = ChainGeneses.POLKADOT_ASSET_HUB
    private val defaultAssetId = "DOT"

    override fun configure(payload: ClaimGiftDeepLinkData, type: DeepLinkConfigurator.Type): Uri {
        val data = if (payload.chainId == defaultChainId) {
            if (payload.symbol.value == defaultAssetId) {
                payload.seed
            } else {
                makePayload(payload.seed, payload.symbol.value)
            }
        } else {
            val normalizedChainId = normaliseChainId(payload.chainId)
            makePayload(payload.seed, payload.symbol.value, normalizedChainId)
        }

        return linkBuilderFactory.newLink(type)
            .setAction(action)
            .setScreen(screen)
            .addParamIfNotNull(payloadParam, data)
            .build()
    }

    fun normaliseChainId(chainId: String): String {
        val noPrefixChainId = chainId.removePrefix("eip155:")
        val substringEnd = min(noPrefixChainId.length, chainIdLength)
        return noPrefixChainId.substring(startIndex = 0, endIndex = substringEnd)
    }

    fun fromPayload(payload: String): ClaimGiftPayloadParams {
        val (seed, symbol: String?, chainIdPrefix: String?) = payload.split("_")

        return ClaimGiftPayloadParams(
            seed = seed,
            symbol = symbol?.let { TokenSymbol(it) },
            chainIdPrefix = chainIdPrefix
        )
    }

    private fun makePayload(vararg params: String): String {
        return params.joinToString("_")
    }
}
