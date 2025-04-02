package io.novafoundation.nova.common.utils.webView

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class NovaCardWebViewClientFactory {
    fun create(interceptors: List<WebViewRequestInterceptor>) = NovaCardWebViewClient(interceptors)
}

class NovaCardWebViewClient(private val interceptors: List<WebViewRequestInterceptor>) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        interceptors.firstOrNull { it.intercept(request) }

        return super.shouldInterceptRequest(view, request)
    }
}
