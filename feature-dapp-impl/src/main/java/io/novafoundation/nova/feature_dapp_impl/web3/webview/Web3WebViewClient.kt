package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import io.novafoundation.nova.common.utils.setVisible
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
        onPageChangedListener: OnPageChangedListener,
    ): Web3WebViewClient {
        return Web3WebViewClient(injectors, extensionStore, webView, onPageChangedListener)
    }
}

typealias OnPageChangedListener = (url: String, title: String?) -> Unit

class Web3WebViewClient(
    private val injectors: List<Web3Injector>,
    private val extensionStore: ExtensionsStore,
    private val webView: WebView,
    private val onPageChangedListener: OnPageChangedListener,
) : WebViewClient() {

    fun initialInject() {
        injectors.forEach { it.initialInject(webView, extensionStore) }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        tryInject(webView, url)
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        onPageChangedListener(url, view.title)
    }

    private fun tryInject(view: WebView, url: String) = injectors.forEach { it.injectForPage(view, url, extensionStore) }
}

private const val MAX_PROGRESS = 100

class Web3ChromeClient(
    private val fileChooser: WebViewFileChooser,
    private val progressBar: ProgressBar
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        progressBar.progress = newProgress

        progressBar.setVisible(newProgress < MAX_PROGRESS)
    }

    override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
        fileChooser.onShowFileChooser(filePathCallback, fileChooserParams)
        return true
    }
}
