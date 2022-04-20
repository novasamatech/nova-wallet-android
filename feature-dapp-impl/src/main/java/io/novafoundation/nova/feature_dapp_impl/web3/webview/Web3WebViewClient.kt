package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

interface Web3Injector {

    fun initialInject(into: WebView, extensionStore: ExtensionsStore)

    fun injectForPage(into: WebView, url: String, extensionStore: ExtensionsStore)
}

class Web3WebViewClientFactory(
    private val injectors: List<Web3Injector>,
) {

    fun create(
        webView: WebView,
        extensionStore: ExtensionsStore,
        onPageChangedListener: OnPageChangedListener,
        coroutineScope: CoroutineScope,
    ): Web3WebViewClient {
        return Web3WebViewClient(injectors, extensionStore, webView, onPageChangedListener, coroutineScope)
    }
}

typealias OnPageChangedListener = (url: String, title: String?) -> Unit

class Web3WebViewClient(
    private val injectors: List<Web3Injector>,
    private val extensionStore: ExtensionsStore,
    private val webView: WebView,
    private val onPageChangedListener: OnPageChangedListener,
    private val coroutineScope: CoroutineScope,
) : WebViewClient() {

    fun initialInject() {
        injectors.forEach { it.initialInject(webView, extensionStore) }
    }

    @OptIn(ExperimentalTime::class)
    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        tryInject(webView, url)
    }

    @OptIn(ExperimentalTime::class)
    override fun onPageFinished(view: WebView, url: String) {
        tryInject(webView, url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        onPageChangedListener(url, view.title)
    }

    private fun tryInject(view: WebView, url: String) = coroutineScope.launch(Dispatchers.Default) {
        injectors.forEach { it.injectForPage(view, url, extensionStore) }
    }
}
