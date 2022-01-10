package io.novafoundation.nova.feature_dapp_impl.web3.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView
import io.novafoundation.nova.common.BuildConfig

@SuppressLint("SetJavaScriptEnabled")
fun WebView.injectWeb3(
    web3ClientFactory: Web3WebViewClientFactory,
) {
    settings.javaScriptEnabled = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.builtInZoomControls = true
    settings.displayZoomControls = false
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.domStorageEnabled = true
    settings.javaScriptCanOpenWindowsAutomatically = true

    val web3Client = web3ClientFactory.create(this)
    web3Client.initialInject()

    webViewClient = web3Client

    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

    settings.userAgentString = settings.userAgentString + "NovaWallet(Platform=Android)"
}

fun WebView.uninjectWeb3() {
    settings.javaScriptEnabled = false
}

@OptIn(ExperimentalStdlibApi::class)
private val ADDITIONAL_HEADERS = buildMap<String, String> {
    put("Connection", "close")
    put("Content-Type", "text/plain")
    put("Access-Control-Allow-Origin", "*")
    put("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS")
    put("Access-Control-Max-Age", "600")
    put("Access-Control-Allow-Credentials", "true")
    put("Access-Control-Allow-Headers", "accept, authorization, Content-Type")
}

class Web3WebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : WebView(context, attrs) {

    override fun loadUrl(url: String) {
        super.loadUrl(url, ADDITIONAL_HEADERS)
    }
}
