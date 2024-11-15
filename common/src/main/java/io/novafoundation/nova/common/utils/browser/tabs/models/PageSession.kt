package io.novafoundation.nova.common.utils.browser.tabs.models

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
