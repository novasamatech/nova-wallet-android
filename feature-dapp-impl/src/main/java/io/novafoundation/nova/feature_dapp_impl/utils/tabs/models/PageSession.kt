package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebView
import io.novafoundation.nova.feature_dapp_impl.web3.webview.PageCallback
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3ChromeClient
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClient
import io.novafoundation.nova.feature_dapp_impl.web3.webview.injectWeb3
import io.novafoundation.nova.feature_dapp_impl.web3.webview.uninjectWeb3
import java.util.Date

class PageSessionFactory {

    fun create(tabId: String, sessionStartTime: Date, startUrl: String): PageSession {
        return PageSession(
            tabId = tabId,
            sessionStartTime = sessionStartTime,
            startUrl = startUrl
        )
    }
}

class PageSession(
    val tabId: String,
    val sessionStartTime: Date,
    val startUrl: String
) : PageCallback {

    private var _webView: WebView? = null
    val webView: WebView
        get() {
            return _webView ?: throw IllegalStateException("WebView is not initialized")
        }

    private var _webViewClient: Web3WebViewClient? = null
    val webViewClient: Web3WebViewClient
        get() {
            return _webViewClient ?: throw IllegalStateException("WebViewClient is not initialized")
        }

    private var nestedPageCallback: PageCallback? = null

    fun initialize(context: Context, onInitialized: (WebView) -> Unit) {
        if (_webView == null) {
            _webView = WebView(context)
            _webViewClient = Web3WebViewClient(
                webView = _webView!!,
                pageCallback = this
            )

            webView.injectWeb3(webViewClient)

            onInitialized(webView)
        }
    }

    fun attachSession(
        chromeClient: Web3ChromeClient,
        pageCallback: PageCallback
    ) {
        webView.webChromeClient = chromeClient
        this.nestedPageCallback = pageCallback
    }

    fun detachSession() {
        webView.webChromeClient = null
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

    fun destroySession() {
        webView.uninjectWeb3()
        webView.destroy()
    }
}
