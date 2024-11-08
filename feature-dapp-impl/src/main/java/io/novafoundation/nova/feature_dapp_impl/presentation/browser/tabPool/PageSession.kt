package io.novafoundation.nova.feature_dapp_impl.presentation.browser.tabPool

import android.content.Context
import android.webkit.WebView
import android.webkit.WebView.enableSlowWholeDocumentDraw

interface PageSession {

    val webView: WebView

}

class RealPageSession(
    context: Context,
    private val startUrl: String?
) : PageSession {

    override val webView by lazy {
        WebView(context).apply {
            startUrl?.let { loadUrl(it) }
        }
    }

}
