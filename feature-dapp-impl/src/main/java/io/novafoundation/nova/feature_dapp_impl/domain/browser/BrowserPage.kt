package io.novafoundation.nova.feature_dapp_impl.domain.browser

class BrowserPage(
    val display: String,
    val synchronizedWithBrowser: Boolean,
    val security: Security
) {

    enum class Security {
        SECURE, DANGEROUS, UNKNOWN
    }
}

val BrowserPage.isSecure
    get() = security == BrowserPage.Security.SECURE

val BrowserPage.isDangerous
    get() = security == BrowserPage.Security.DANGEROUS
