package io.novafoundation.nova.feature_dapp_impl.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object UrlNormalizer {

    /**
     * @return normalized url in a form of protocol://host
     */
    suspend fun normalizeUrl(url: String) = withContext(Dispatchers.Default) {
        val parsedUrl = URL(url)

        "${parsedUrl.protocol}://${parsedUrl.host}"
    }
}
