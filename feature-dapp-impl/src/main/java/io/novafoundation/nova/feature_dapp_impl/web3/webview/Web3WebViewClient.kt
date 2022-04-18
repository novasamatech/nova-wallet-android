package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.webkit.WebView
import android.webkit.WebViewClient
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionsStore

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
        onPageChangedListener: OnPageChangedListener
    ): Web3WebViewClient {
        return Web3WebViewClient(injectors, extensionStore, webView, onPageChangedListener)
    }
}

typealias OnPageChangedListener = (url: String, title: String?) -> Unit

class Web3WebViewClient(
    private val injectors: List<Web3Injector>,
    private val extensionStore: ExtensionsStore,
    private val webView: WebView,
    private val onPageChangedListener: OnPageChangedListener
) : WebViewClient() {

    fun initialInject() {
        injectors.forEach { it.initialInject(webView, extensionStore) }
    }

    // onLoadResource() appears to be more reliable then onPageStart() and onPageFinished() combined for injection js
    override fun onLoadResource(view: WebView?, url: String) {
        super.onLoadResource(view, url)
        tryInject(webView, url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        onPageChangedListener(url, view.title)
    }

    private fun tryInject(view: WebView, url: String) {
        injectors.forEach { it.injectForPage(view, url, extensionStore) }
    }
}
