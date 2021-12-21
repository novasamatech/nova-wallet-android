package io.novafoundation.nova.feature_dapp_impl.web3

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import io.novafoundation.nova.common.BuildConfig

@SuppressLint("SetJavaScriptEnabled")
fun WebView.prepareForWeb3() {
    settings.javaScriptEnabled = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.builtInZoomControls = true
    settings.displayZoomControls = false
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.domStorageEnabled = true
    settings.javaScriptCanOpenWindowsAutomatically = true

    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

    settings.userAgentString = settings.userAgentString + "NovaWallet(Platform=Android)"
}
