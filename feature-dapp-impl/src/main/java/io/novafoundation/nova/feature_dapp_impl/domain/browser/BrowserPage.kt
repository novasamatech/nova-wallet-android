package io.novafoundation.nova.feature_dapp_impl.domain.browser

class BrowserPage(
    val url: String,
    val title: String?,
    val synchronizedWithBrowser: Boolean
)

class BrowserPageAnalyzed(
    val display: String,
    val title: String?,
    val url: String,
    val synchronizedWithBrowser: Boolean,
    val isFavourite: Boolean,
    val security: Security
) {

    enum class Security {
        SECURE, DANGEROUS, UNKNOWN
    }
}

val BrowserPageAnalyzed.isSecure
    get() = security == BrowserPageAnalyzed.Security.SECURE

val BrowserPageAnalyzed.isDangerous
    get() = security == BrowserPageAnalyzed.Security.DANGEROUS
