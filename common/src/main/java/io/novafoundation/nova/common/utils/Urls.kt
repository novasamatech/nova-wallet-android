package io.novafoundation.nova.common.utils

import android.util.Patterns
import java.net.URL

object Urls {

    const val HTTP_PREFIX = "http://"
    const val HTTPS_PREFIX = "https://"

    /**
     * @return normalized url in a form of protocol://host
     */
    fun normalizeUrl(url: String): String {
        val parsedUrl = URL(url)

        return "${parsedUrl.protocol}://${parsedUrl.host}"
    }

    fun hostOf(url: String): String {
        return URL(url).host
    }

    fun isValidWebUrl(url: String): Boolean {
        return Patterns.WEB_URL.matcher(url).matches()
    }

    fun ensureHttpsProtocol(url: String): String {
        return when {
            url.startsWith(HTTPS_PREFIX) -> url
            url.startsWith(HTTP_PREFIX) -> url.replace(HTTP_PREFIX, HTTPS_PREFIX)
            else -> "$HTTPS_PREFIX$url"
        }
    }
}

val URL.isSecure
    get() = protocol == "https"
