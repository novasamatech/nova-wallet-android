package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

interface NovaCardInterceptor {

    /**
     * @return Make a request and return WebResourceResponse if the request was intercepted otherwise null
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

/**
 * Pass body here since response inout stream may be already closed
 */
fun Response.toWebResourceResponse(): WebResourceResponse {
    val contentType = this.header("Content-Type", null)
    val charset = this.header("Content-Encoding", null)
    val headers = this.headers.toMap()
    val statusMessage = if (this.isSuccessful) "OK" else "Error"

    return WebResourceResponse(
        contentType,
        charset,
        this.code,
        statusMessage,
        headers,
        body?.byteStream()
    )
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
