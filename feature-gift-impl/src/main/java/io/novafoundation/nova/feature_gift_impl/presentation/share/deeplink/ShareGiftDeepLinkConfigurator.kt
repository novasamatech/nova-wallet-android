package io.novafoundation.nova.feature_gift_impl.presentation.share.deeplink

import android.net.Uri
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.DeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.addParamIfNotNull

class ShareGiftDeepLinkData(
    val seed: String,
    val chainId: String,
    val symbol: TokenSymbol
)

class ShareGiftDeepLinkConfigurator(
    private val linkBuilderFactory: LinkBuilderFactory
) : DeepLinkConfigurator<ShareGiftDeepLinkData> {

    val action = "open"
    val screen = "gift"
    val deepLinkPrefix = "/$action/$screen"
    val dataParam = "data"

    override fun configure(payload: ShareGiftDeepLinkData, type: DeepLinkConfigurator.Type): Uri {
        val shortenChainId = payload.chainId.substring(startIndex = 0, endIndex = 6)
        val data = "${payload.seed}_${shortenChainId}_${payload.symbol.value}"
        return linkBuilderFactory.newLink(type)
            .setAction(action)
            .setScreen(screen)
            .addParamIfNotNull(dataParam, data)
            .build()
    }
}
