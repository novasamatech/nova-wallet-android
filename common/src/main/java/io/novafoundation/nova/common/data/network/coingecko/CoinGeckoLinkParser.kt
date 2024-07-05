package io.novafoundation.nova.common.data.network.coingecko

import android.net.Uri
import io.novafoundation.nova.common.utils.Urls

private const val COINGECKO_HOST = "www.coingecko.com"
private const val COINGECKO_PATH_LANGUAGE = "en"
private const val COINGECKO_PATH_SEGMENT = "coins"

class CoinGeckoLinkParser {

    class Content(val priceId: String)

    fun parse(input: String): Result<Content> = runCatching {
        val parsedUri = parseToUri(input)

        require(parsedUri.host == COINGECKO_HOST)
        val (language, coinSegment, priceId) = parsedUri.pathSegments
        require(coinSegment == COINGECKO_PATH_SEGMENT)

        Content(priceId)
    }

    fun format(priceId: String): String {
        return Uri.Builder()
            .scheme("https")
            .authority(COINGECKO_HOST)
            .appendPath(COINGECKO_PATH_LANGUAGE)
            .appendPath(COINGECKO_PATH_SEGMENT)
            .appendPath(priceId)
            .build()
            .toString()
    }

    private fun parseToUri(input: String): Uri {
        val withProtocol = Urls.ensureHttpsProtocol(input)
        return Uri.parse(withProtocol)
    }
}
