package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient

interface Web3Controller {

    fun initialInject(into: WebView)

    fun injectForPage(into: WebView, url: String)
}

class Web3WebViewClientFactory(
    private val controllers: List<Web3Controller>,
) {

    fun create(webView: WebView) = Web3WebViewClient(controllers, webView)
}

class Web3WebViewClient(
    private val controllers: List<Web3Controller>,
    private val webView: WebView,
) : WebViewClient() {

    fun initialInject() {
        controllers.forEach { it.initialInject(webView) }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        controllers.forEach { it.injectForPage(view, url) }
    }
}
