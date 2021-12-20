package io.novafoundation.nova.feature_dapp_impl.web3

import android.webkit.WebView

class WebViewHolder {

    var webView: WebView? = null
        private set

    fun set(new: WebView) {
        webView = new
    }

    fun release() {
        webView = null
    }
}
