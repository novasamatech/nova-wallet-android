package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import android.Manifest
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.sha512
import io.novafoundation.nova.common.utils.systemCall.FilePickerSystemCall
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.model.CardSetupConfig
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.NovaCardInterceptor
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NovaCardWebViewControllerFactory(
    private val systemCallExecutor: SystemCallExecutor,
    private val fileProvider: FileProvider,
    private val permissionsAskerFactory: PermissionsAskerFactory,
    private val appLinksProvider: AppLinksProvider,
    private val widgetId: String
) {

    fun create(
        fragment: Fragment,
        webView: WebView,
        interceptors: List<NovaCardInterceptor>,
        setupConfig: CardSetupConfig,
        scope: CoroutineScope,
    ): NovaCardWebViewController {
        return NovaCardWebViewController(
            fragment = fragment,
            webView = webView,
            fileProvider = fileProvider,
            appLinksProvider = appLinksProvider,
            permissionsAskerFactory = permissionsAskerFactory,
            systemCallExecutor = systemCallExecutor,
            setupConfig = setupConfig,
            widgetId = widgetId,
            interceptors = interceptors,
            coroutineScope = scope
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
    private val setupConfig: CardSetupConfig,
    private val widgetId: String,
    private val interceptors: List<NovaCardInterceptor>,
    private val coroutineScope: CoroutineScope
) {

    private val permissionsAsker = permissionsAskerFactory.create(fragment)

    private val webViewClient = object : WebViewClient() {

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            interceptors.firstOrNull { it.intercept(request) }

            return super.shouldInterceptRequest(view, request)
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

        webView.webViewClient = webViewClient

        webView.webChromeClient = webChromeClient

        val uri = Uri.parse(appLinksProvider.novaCardWidgetUrl).buildUpon()
            .appendQueryParameter("widget_id", widgetId)
            .appendQueryParameter("type", "sell")
            .appendQueryParameter("currency", setupConfig.spendToken.symbol.value)
            .appendQueryParameter("payment_method", "fiat_card_open")
            .appendQueryParameter("fiat_currency", "EUR")
            .appendQueryParameter("theme", "nova")
            .appendQueryParameter("hide_refund_address", "true")
            .appendQueryParameter("refund_address", setupConfig.refundAddress)
            .appendQueryParameter("fix_payment_method", "true")
            .appendQueryParameter("show_spend_card_details", "true")
            .build()

        webView.loadUrl(uri.toString())
    }
}
