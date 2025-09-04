package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.feature_dapp_impl.web3.webview.PageCallback
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3ChromeClient
import io.novafoundation.nova.feature_dapp_impl.web3.webview.CompoundWeb3Injector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClient
import io.novafoundation.nova.feature_dapp_impl.web3.webview.injectWeb3
import io.novafoundation.nova.feature_dapp_impl.web3.webview.uninjectWeb3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrowserTabSessionFactory(
    private val compoundWeb3Injector: CompoundWeb3Injector,
    private val contextManager: ContextManager
) {

    suspend fun create(tabId: String, startUrl: String, onPageChangedCallback: OnPageChangedCallback): BrowserTabSession {
        return withContext(Dispatchers.Main) {
            val context = contextManager.getActivity()!!
            val webView = WebView(context)

            BrowserTabSession(
                tabId = tabId,
                startUrl = startUrl,
                webView = webView,
                compoundWeb3Injector = compoundWeb3Injector,
                onPageChangedCallback = onPageChangedCallback
            )
        }
    }
}

class BrowserTabSession(
    val tabId: String,
    val startUrl: String,
    val webView: WebView,
    compoundWeb3Injector: CompoundWeb3Injector,
    private val onPageChangedCallback: OnPageChangedCallback
) : PageCallback {

    val webViewClient: Web3WebViewClient = Web3WebViewClient(
        webView = webView,
        pageCallback = this
    )

    var currentUrl: String = startUrl

    private var nestedPageCallback: PageCallback? = null

    init {
        webView.injectWeb3(webViewClient)
        webView.loadUrl(startUrl)
        compoundWeb3Injector.initialInject(webView)
    }

    fun attachToHost(
        chromeClient: Web3ChromeClient,
        pageCallback: PageCallback
    ) {
        webView.webChromeClient = chromeClient
        this.nestedPageCallback = pageCallback

        // To provide initial state
        pageCallback.onPageChanged(webView, webView.url, webView.title)
    }

    fun detachFromHost() {
        nestedPageCallback = null
        webView.webChromeClient = null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        nestedPageCallback?.onPageStarted(view, url, favicon)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return nestedPageCallback?.shouldOverrideUrlLoading(view, request) ?: false
    }

    override fun onPageChanged(view: WebView, url: String?, title: String?) {
        nestedPageCallback?.onPageChanged(view, url, title)
        onPageChangedCallback.onPageChanged(tabId, url, title)
    }

    fun destroy() {
        webView.uninjectWeb3()
        webView.destroy()
    }
}
