package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import android.Manifest
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.readText
import io.novafoundation.nova.common.utils.systemCall.FilePickerSystemCall
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.model.CardSetupConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.time.Duration.Companion.milliseconds

class NovaCardWebViewControllerFactory(
    private val systemCallExecutor: SystemCallExecutor,
    private val fileProvider: FileProvider,
    private val permissionsAskerFactory: PermissionsAskerFactory,
    private val appLinksProvider: AppLinksProvider,
    private val gson: Gson,
    private val widgetId: String,
    private val okHttpClient: OkHttpClient,
) {

    fun create(
        fragment: Fragment,
        webView: WebView,
        eventHandler: NovaCardEventHandler,
        setupConfig: CardSetupConfig,
        scope: CoroutineScope,
    ): NovaCardWebViewController {
        val jsCallback = NovaCardJsCallback(gson, eventHandler)
        val pageProvider = NovaCardWebPageProvider(widgetId = widgetId, setupConfig)
        return NovaCardWebViewController(
            fragment = fragment,
            webView = webView,
            fileProvider = fileProvider,
            appLinksProvider = appLinksProvider,
            permissionsAskerFactory = permissionsAskerFactory,
            systemCallExecutor = systemCallExecutor,
            pageProvider = pageProvider,
            novaCardJsCallback = jsCallback,
            coroutineScope = scope,
            okHttpClient = okHttpClient
        )
    }
}

class NovaCardWebViewController(
    private val fragment: Fragment,
    private val webView: WebView,
    private val fileProvider: FileProvider,
    private val appLinksProvider: AppLinksProvider,
    private val permissionsAskerFactory: PermissionsAskerFactory,
    private val systemCallExecutor: SystemCallExecutor,
    private val pageProvider: NovaCardWebPageProvider,
    private val novaCardJsCallback: NovaCardJsCallback,
    private val coroutineScope: CoroutineScope,
    private val okHttpClient: OkHttpClient,
) {

    private val permissionsAsker = permissionsAskerFactory.create(fragment)

    private val webViewClient = object : WebViewClient() {

        private var jsScriptWasCalled = false
        private var interceptedCardRequest: Request.Builder? = null

        init {
            coroutineScope.launch(Dispatchers.IO) {
                repeat(100) {
                    if (interceptedCardRequest != null) {
                        val okHttpResponse = okHttpClient.newCall(interceptedCardRequest!!.build()).execute()

                        if (okHttpResponse.isSuccessful) {
                            val responseBody = okHttpResponse.body

                            val data = responseBody!!.byteStream().readText()

                            Log.d("NovaCard", "Polled $data")
                        } else {
                            Log.d("NovaCard", "Polling failed")
                        }
                    }

                    delay(1000.milliseconds)
                }
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

            if (!jsScriptWasCalled) {
                jsScriptWasCalled = true
                webView.evaluateJavascript(pageProvider.getJsScript(), null)
            }
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val url = request.url.toString()

            // Intercept requests that might return a JSON response
            if (url.contains("https://api.mercuryo.io/v1.6/cards")) { // Specify your condition here
                return performOkHttpRequest(request)
            }

            return super.shouldInterceptRequest(view, request)
        }

        private fun performOkHttpRequest(request: WebResourceRequest): WebResourceResponse? {
            try {
                // Create OkHttp Request based on WebResourceRequest
                val okHttpRequestBuilder = Request.Builder().url(request.url.toString())

                // Set method (GET, POST, etc.) and request body if needed
                when (request.method) {
                    "GET" -> okHttpRequestBuilder.get()
                    else -> okHttpRequestBuilder.get()
                }

                // Add headers from WebResourceRequest
                for ((key, value) in request.requestHeaders) {
                    okHttpRequestBuilder.addHeader(key, value)
                }

                val cookieManager = CookieManager.getInstance()
                val cookies = cookieManager.getCookie(request.url.toString())
                if (cookies != null) {
                    okHttpRequestBuilder.addHeader("Cookie", cookies)
                }

                interceptedCardRequest = okHttpRequestBuilder

                // Execute the OkHttp request
                val okHttpResponse = okHttpClient.newCall(okHttpRequestBuilder.build()).execute()

                // Check if the response is successful
                return if (okHttpResponse.isSuccessful) {
                    val responseBody = okHttpResponse.body

                    val data = responseBody!!.byteStream().readText()

                    Log.d("NovaCard", "Intercepted $data")

                    // Return WebResourceResponse with the intercepted data
                    null
                } else {
                    // Return null or handle error cases (e.g., 404, 500)
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null // Fall back to default WebView behavior if something goes wrong
        }
    }

    private val webChromeClient = object : android.webkit.WebChromeClient() {

        override fun onPermissionRequest(request: PermissionRequest) {
            coroutineScope.launch {
                val result = permissionsAsker.requirePermissionsOrExit(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)

                if (result) {
                    request.grant(request.resources)
                } else {
                    request.deny()
                }
            }
        }

        override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
            coroutineScope.launch {
                val result = systemCallExecutor.executeSystemCall(FilePickerSystemCall(fileProvider))
                result.onSuccess { filePathCallback.onReceiveValue(arrayOf(it)) }
                    .onFailure { filePathCallback.onReceiveValue(null) }
            }

            return true
        }
    }

    fun setup() {
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.useWideViewPort = true
        webSettings.displayZoomControls = false
        webView.addJavascriptInterface(novaCardJsCallback, pageProvider.getCallbackName())

        webView.webViewClient = webViewClient

        webView.webChromeClient = webChromeClient

        webView.loadDataWithBaseURL(
            appLinksProvider.novaCardWidgetUrl,
            pageProvider.getPage(),
            "text/html",
            "UTF-8",
            null
        )
    }
}
