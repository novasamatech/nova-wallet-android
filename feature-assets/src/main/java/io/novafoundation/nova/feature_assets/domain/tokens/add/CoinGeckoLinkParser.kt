package io.novafoundation.nova.feature_assets.domain.tokens.add

import android.net.Uri
import io.novafoundation.nova.common.utils.Urls

private const val COINGECKO_HOST = "coingecko.com"
private const val COINGECKO_PATH_SEGMENT = "coins"

class CoinGeckoLinkParser {

    class Content(val priceId: String)

    fun parse(input: String): Result<Content> = runCatching {
        val withProtocol = Urls.ensureHttpsProtocol(input)

        val parsedUri = Uri.parse(withProtocol)

        require(parsedUri.host == COINGECKO_HOST)
        val (coinSegment, priceId) = parsedUri.pathSegments
        require(coinSegment == COINGECKO_PATH_SEGMENT)

        Content(priceId)
    }
}
