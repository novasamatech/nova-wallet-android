package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.feature_dapp_impl.utils.integrityCheck.IntegrityCheckJSBridge
import io.novafoundation.nova.feature_dapp_impl.utils.integrityCheck.IntegrityCheckJSBridgeFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.PageCallback
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3ChromeClient
import io.novafoundation.nova.feature_dapp_impl.web3.webview.CompoundWeb3Injector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClient
import io.novafoundation.nova.feature_dapp_impl.web3.webview.injectWeb3
import io.novafoundation.nova.feature_dapp_impl.web3.webview.uninjectWeb3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class BrowserTabSessionFactory(
    private val compoundWeb3Injector: CompoundWeb3Injector,
    private val contextManager: ContextManager,
    private val integrityCheckJSBridgeFactory: IntegrityCheckJSBridgeFactory
) {

    suspend fun create(tabId: String, startUrl: String, onPageChangedCallback: OnPageChangedCallback): BrowserTabSession {
        return withContext(Dispatchers.Main) {
            val context = contextManager.getActivity()!!
            val webView = WebView(context)
            val integrityCheckProvider = integrityCheckJSBridgeFactory.create(webView)

            BrowserTabSession(
                tabId = tabId,
                startUrl = startUrl,
                webView = webView,
                integrityCheckJSBridge = integrityCheckProvider,
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
    private val onPageChangedCallback: OnPageChangedCallback,
    private val integrityCheckJSBridge: IntegrityCheckJSBridge
) : PageCallback, CoroutineScope by CoroutineScope(Dispatchers.Main) {

    val webViewClient: Web3WebViewClient = Web3WebViewClient(
        webView = webView,
        pageCallback = this
    )

    var currentUrl: String = startUrl

    private var sessionCallback: SessionCallback? = null

    init {
        webView.injectWeb3(webViewClient)
        webView.loadUrl(startUrl)
        compoundWeb3Injector.initialInject(webView)

        integrityCheckJSBridge.errorFlow
            .onEach { sessionCallback?.onPageError(it) }
            .launchIn(this)
    }

    fun attachToHost(
        chromeClient: Web3ChromeClient,
        pageCallback: SessionCallback
    ) {
        webView.webChromeClient = chromeClient
        this.sessionCallback = pageCallback

        // To provide initial state
        pageCallback.onPageChanged(webView, webView.url, webView.title)
    }

    fun detachFromHost() {
        sessionCallback = null
        webView.webChromeClient = null
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        sessionCallback?.onPageStarted(view, url, favicon)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return sessionCallback?.shouldOverrideUrlLoading(view, request) ?: false
    }

    override fun onPageChanged(view: WebView, url: String?, title: String?) {
        sessionCallback?.onPageChanged(view, url, title)
        onPageChangedCallback.onPageChanged(tabId, url, title)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        integrityCheckJSBridge.onPageFinished()
    }

    fun destroy() {
        cancel() // Cancel coroutine scope
        integrityCheckJSBridge.onDestroy()
        webView.uninjectWeb3()
        webView.destroy()
    }
}
