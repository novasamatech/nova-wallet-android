package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import io.novafoundation.nova.common.BuildConfig
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionsStore
import kotlinx.coroutines.CoroutineScope

@SuppressLint("SetJavaScriptEnabled")
fun WebView.injectWeb3(
    web3ClientFactory: Web3WebViewClientFactory,
    extensionsStore: ExtensionsStore,
    onPageChanged: OnPageChangedListener,
    coroutineScope: CoroutineScope,
) {
    settings.javaScriptEnabled = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.builtInZoomControls = true
    settings.displayZoomControls = false
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.domStorageEnabled = true
    settings.javaScriptCanOpenWindowsAutomatically = true

    val web3Client = web3ClientFactory.create(this, extensionsStore, onPageChanged, coroutineScope)
    web3Client.initialInject()

    webViewClient = web3Client

    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

    settings.userAgentString = settings.userAgentString + "NovaWallet(Platform=Android)"
}

fun WebView.uninjectWeb3() {
    settings.javaScriptEnabled = false
}
