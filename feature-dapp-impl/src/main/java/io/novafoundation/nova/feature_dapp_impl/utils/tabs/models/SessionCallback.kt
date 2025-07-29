package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView

interface SessionCallback {

    fun onPageStarted(webView: WebView, url: String, favicon: Bitmap?)

    fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean

    fun onPageChanged(webView: WebView, url: String?, title: String?)

    fun onPageError(error: String)
}
