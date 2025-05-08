package io.novafoundation.nova.common.utils.webView

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

interface WebViewRequestInterceptor {

    /**
     * @return Intercept a request and return WebResourceResponse if the true was intercepted otherwise false
     */
    fun intercept(request: WebResourceRequest): Boolean
}

fun OkHttpClient.makeRequestBlocking(requestBuilder: Request.Builder): Response {
    val okHttpResponse = this.newCall(requestBuilder.build()).execute()

    if (okHttpResponse.isSuccessful) {
        return okHttpResponse
    }

    throw RuntimeException("Request failed with ${okHttpResponse.code} code: ${okHttpResponse.networkResponse?.body}")
}

fun WebResourceRequest.toOkHttpRequestBuilder(): Request.Builder {
    val url = url.toString()
    val okHttpRequestBuilder = Request.Builder().url(url)

    okHttpRequestBuilder.get()

    for ((key, value) in requestHeaders) {
        okHttpRequestBuilder.addHeader(key, value)
    }

    val cookieManager = CookieManager.getInstance()
    val cookies = cookieManager.getCookie(url)
    if (cookies != null) {
        okHttpRequestBuilder.addHeader("Cookie", cookies)
    }

    return okHttpRequestBuilder
}
