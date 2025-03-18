package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.NovaCardInterceptor

class NovaCardWebViewClientFactory {
    fun create(interceptors: List<NovaCardInterceptor>) = NovaCardWebViewClient(interceptors)
}

class NovaCardWebViewClient(private val interceptors: List<NovaCardInterceptor>) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        interceptors.firstOrNull { it.intercept(request) }

        return super.shouldInterceptRequest(view, request)
    }
}
