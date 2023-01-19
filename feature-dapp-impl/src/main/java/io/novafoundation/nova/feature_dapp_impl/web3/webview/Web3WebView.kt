package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import io.novafoundation.nova.common.BuildConfig

@SuppressLint("SetJavaScriptEnabled")
fun WebView.injectWeb3(
    progressBar: ProgressBar,
    fileChooser: WebViewFileChooser,
    web3Client: Web3WebViewClient
) {
    settings.javaScriptEnabled = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.builtInZoomControls = true
    settings.displayZoomControls = false
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.domStorageEnabled = true
    settings.javaScriptCanOpenWindowsAutomatically = true

    web3Client.initialInject()
    this.webViewClient = web3Client
    webChromeClient = Web3ChromeClient(fileChooser, progressBar)

    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
}

fun WebView.changeUserAgentByDesktopMode(desktopMode: Boolean) {
    val defaultUserAgent = WebSettings.getDefaultUserAgent(context)

    settings.userAgentString = if (desktopMode) {
        "Mozilla/5.0 (X11; CrOS x86_64 10066.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
    } else {
        defaultUserAgent
    }
}

fun WebView.uninjectWeb3() {
    settings.javaScriptEnabled = false

    webChromeClient = null
}
