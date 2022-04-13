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

    fun create(
        webView: WebView,
        onPageChangedListener: OnPageChangedListener
    ): Web3WebViewClient {
        return Web3WebViewClient(controllers, webView, onPageChangedListener)
    }
}

typealias OnPageChangedListener = (url: String, title: String?) -> Unit

class Web3WebViewClient(
    private val controllers: List<Web3Controller>,
    private val webView: WebView,
    private val onPageChangedListener: OnPageChangedListener
) : WebViewClient() {

    fun initialInject() {
        controllers.forEach { it.initialInject(webView) }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        tryInject(view, url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        tryInject(view, url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        onPageChangedListener(url, view.title)
    }

    // we try to inject both at `onPageStarted` and `onPageFinished` since
    // since both of them are not sufficient by their own
    // (several dapps tries to detect extension before onPageFinished, some others does not have document ready at `onPageStarted`)
    private fun tryInject(view: WebView, url: String) {
        controllers.forEach { it.injectForPage(view, url) }
    }
}
