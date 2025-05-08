package io.novafoundation.nova.common.utils.webView.interceptors

import android.webkit.WebResourceRequest
import io.novafoundation.nova.common.utils.webView.WebViewRequestInterceptor

class CompoundWebViewRequestInterceptor(
    private val interceptors: List<WebViewRequestInterceptor>
) : WebViewRequestInterceptor {

    constructor(vararg interceptors: WebViewRequestInterceptor) : this(interceptors.toList())

    override fun intercept(request: WebResourceRequest): Boolean {
        return interceptors.any { it.intercept(request) }
    }
}
