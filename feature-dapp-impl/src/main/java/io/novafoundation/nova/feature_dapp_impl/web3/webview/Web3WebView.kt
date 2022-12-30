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
    webViewClient: Web3WebViewClient
) {
    settings.javaScriptEnabled = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.builtInZoomControls = true
    settings.displayZoomControls = false
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.domStorageEnabled = true
    settings.javaScriptCanOpenWindowsAutomatically = true

    webViewClient.initialInject()
    this.webViewClient = webViewClient
    webChromeClient = Web3ChromeClient(fileChooser, progressBar)

    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    settings.userAgentString = modifyUserAgent(settings.userAgentString)
    reload()
}

fun WebView.setDesktopMode(desktopMode: Boolean) {
    val defaultUserAgent = WebSettings.getDefaultUserAgent(context)
    val userAgent = if (desktopMode) {
        try {
            val androidString: String = defaultUserAgent.substring(defaultUserAgent.indexOf("("), defaultUserAgent.indexOf(")") + 1)
            defaultUserAgent.replace(androidString, "X11; Linux x86_64")
        } catch (e: Exception) {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
        }
    } else {
        defaultUserAgent
    }

    settings.userAgentString = modifyUserAgent(userAgent)
}

fun WebView.uninjectWeb3() {
    settings.javaScriptEnabled = false

    webChromeClient = null
    webChromeClient = null
}

private fun modifyUserAgent(userAgent: String): String {
    return "$userAgent NovaWallet(Platform=Android)"
}
