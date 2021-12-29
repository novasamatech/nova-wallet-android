package io.novafoundation.nova.feature_dapp_impl.util

import java.net.URL

object UrlNormalizer {

    /**
     * @return normalized url in a form of protocol://host
     */
    fun normalizeUrl(url: String): String {
        val parsedUrl = URL(url)

        return "${parsedUrl.protocol}://${parsedUrl.host}"
    }
}
