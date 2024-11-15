package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

import android.content.Context
import android.webkit.WebView

class PageSession(
    val tabId: String,
    startUrl: String,
    context: Context
) {

    val webView: WebView = WebView(context).apply {
        loadUrl(startUrl)
    }
}
