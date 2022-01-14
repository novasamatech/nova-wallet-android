package io.novafoundation.nova.feature_dapp_impl.util

import android.util.Patterns
import java.net.URL

object Urls {

    /**
     * @return normalized url in a form of protocol://host
     */
    fun normalizeUrl(url: String): String {
        val parsedUrl = URL(url)

        return "${parsedUrl.protocol}://${parsedUrl.host}"
    }

    fun isValidWebUrl(url: String): Boolean {
        return Patterns.WEB_URL.matcher(url).matches()
    }
}

val URL.isSecure
    get() = protocol == "https"
