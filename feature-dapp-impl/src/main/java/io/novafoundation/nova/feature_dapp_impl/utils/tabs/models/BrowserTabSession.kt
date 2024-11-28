package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebView
import io.novafoundation.nova.feature_dapp_impl.web3.webview.PageCallback
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3ChromeClient
import io.novafoundation.nova.feature_dapp_impl.web3.webview.CompoundWeb3Injector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClient
import io.novafoundation.nova.feature_dapp_impl.web3.webview.injectWeb3
import io.novafoundation.nova.feature_dapp_impl.web3.webview.uninjectWeb3

class BrowserTabSessionFactory(
    private val compoundWeb3Injector: CompoundWeb3Injector
) {

    fun create(tabId: String, startUrl: String): BrowserTabSession {
        return BrowserTabSession(
            tabId = tabId,
            startUrl = startUrl,
            compoundWeb3Injector = compoundWeb3Injector
        )
    }
}

class BrowserTabSession(
    val tabId: String,
    val startUrl: String,
    private val compoundWeb3Injector: CompoundWeb3Injector
) : PageCallback {

    private var _webView: WebView? = null
    val webView: WebView by lazy {
        _webView!!
    }

    private var _webViewClient: Web3WebViewClient? = null
    val webViewClient: Web3WebViewClient by lazy {
        _webViewClient!!
    }

    private var nestedPageCallback: PageCallback? = null

    fun initialize(context: Context) {
        if (_webView == null) {
            _webView = WebView(context)
            _webViewClient = Web3WebViewClient(
                webView = _webView!!,
                pageCallback = this
            )

            webView.injectWeb3(webViewClient)
            webView.loadUrl(startUrl)
            compoundWeb3Injector.initialInject(webView)
        }
    }

    fun attachToHost(
        chromeClient: Web3ChromeClient,
        pageCallback: PageCallback
    ) {
        _webView?.webChromeClient = chromeClient
        this.nestedPageCallback = pageCallback
    }

    fun detachFromHost() {
        _webView?.webChromeClient = null
        nestedPageCallback = null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        nestedPageCallback?.onPageStarted(view, url, favicon)
    }

    override fun handleBrowserIntent(intent: Intent) {
        nestedPageCallback?.handleBrowserIntent(intent)
    }

    override fun onPageChanged(view: WebView, url: String, title: String?) {
        nestedPageCallback?.onPageChanged(view, url, title)
    }

    fun destroy() {
        _webView?.uninjectWeb3()
        _webView?.destroy()
    }
}
