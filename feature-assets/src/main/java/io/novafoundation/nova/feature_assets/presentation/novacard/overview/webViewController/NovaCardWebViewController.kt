package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import android.Manifest
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.systemCall.FilePickerSystemCall
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NovaCardWebViewControllerFactory(
    private val systemCallExecutor: SystemCallExecutor,
    private val fileProvider: FileProvider,
    private val permissionsAskerFactory: PermissionsAskerFactory,
    private val appLinksProvider: AppLinksProvider,
    private val gson: Gson,
    private val widgetId: String
) {
    fun create(
        fragment: Fragment,
        webView: WebView,
        eventHandler: NovaCardEventHandler,
        refundAddress: String,
        coroutineScope: CoroutineScope
    ): NovaCardWebViewController {
        val jsCallback = NovaCardJsCallback(gson, eventHandler)
        val pageProvider = NovaCardWebPageProvider(widgetId = widgetId, refundAddress = refundAddress)
        return NovaCardWebViewController(
            fragment,
            webView,
            fileProvider,
            appLinksProvider,
            permissionsAskerFactory,
            systemCallExecutor,
            pageProvider,
            jsCallback,
            coroutineScope
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
    private val coroutineScope: CoroutineScope
) {

    private val permissionsAsker = permissionsAskerFactory.create(fragment)

    private val webViewClient = object : WebViewClient() {

        private var jsScriptWasCalled = false

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

            if (!jsScriptWasCalled) {
                jsScriptWasCalled = true
                webView.evaluateJavascript(pageProvider.getJsScript(), null)
            }
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
