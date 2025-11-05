package io.novafoundation.nova.feature_gift_impl.presentation.claim.deeplink

import android.net.Uri
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.DeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.addParamIfNotNull

class ClaimGiftDeepLinkData(
    val seed: String,
    val chainId: String,
    val symbol: TokenSymbol
)

class ClaimGiftDeepLinkConfigurator(
    private val linkBuilderFactory: LinkBuilderFactory
) : DeepLinkConfigurator<ClaimGiftDeepLinkData> {

    val action = "open"
    val screen = "gift"
    val deepLinkPrefix = "/$action/$screen"
    val payloadParam = "payload"
    val chainIdLength = 6

    override fun configure(payload: ClaimGiftDeepLinkData, type: DeepLinkConfigurator.Type): Uri {
        val shortenChainId = normaliseChainId(payload.chainId)
            .substring(startIndex = 0, endIndex = chainIdLength)
        val data = "${payload.seed}_${shortenChainId}_${payload.symbol.value}"
        return linkBuilderFactory.newLink(type)
            .setAction(action)
            .setScreen(screen)
            .addParamIfNotNull(payloadParam, data)
            .build()
    }

    fun normaliseChainId(chainId: String): String {
        return chainId.removePrefix("eip155:")
    }
}
