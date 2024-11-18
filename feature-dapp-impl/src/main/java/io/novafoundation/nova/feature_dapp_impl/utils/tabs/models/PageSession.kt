package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

import android.content.Context
import android.webkit.WebView
import java.util.Date

class PageSession(
    val tabId: String,
    val sessionStartTime: Date,
    startUrl: String,
    context: Context
) {
    val webView: WebView = WebView(context).apply {
        loadUrl(startUrl)
    }

    fun destroySession() {
        webView.destroy()
    }
}
