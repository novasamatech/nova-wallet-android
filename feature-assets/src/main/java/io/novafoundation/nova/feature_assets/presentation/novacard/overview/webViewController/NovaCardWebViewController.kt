package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
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
import io.novafoundation.nova.common.utils.sha512
import io.novafoundation.nova.common.utils.systemCall.FilePickerSystemCall
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.model.CardSetupConfig
import io.novafoundation.nova.feature_dapp_core.web3.injector.MetamaskScriptInjector
import io.novafoundation.nova.feature_dapp_core.web3.webView.MetamaskWeb3JavaScriptInterface
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NovaCardWebViewControllerFactory(
    private val systemCallExecutor: SystemCallExecutor,
    private val fileProvider: FileProvider,
    private val permissionsAskerFactory: PermissionsAskerFactory,
    private val appLinksProvider: AppLinksProvider,
    private val gson: Gson,
    private val widgetId: String,
    private val widgetSecret: String,
    private val webViewCardCreationInterceptorFactory: WebViewCardCreationInterceptorFactory,
    private val metamaskScriptInjector: MetamaskScriptInjector,
    private val metamaskWeb3JavaScriptInterface: MetamaskWeb3JavaScriptInterface
) {

    fun create(
        fragment: Fragment,
        webView: WebView,
        eventHandler: NovaCardEventHandler,
        cardCreatedListener: OnCardCreatedListener,
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
            widgetId = widgetId,
            widgetSecret = widgetSecret,
            setupConfig = setupConfig,
            pageProvider = pageProvider,
            novaCardJsCallback = jsCallback,
            coroutineScope = scope,
            cardCreationInterceptor = webViewCardCreationInterceptorFactory.create(cardCreatedListener),
            metamaskScriptInjector = metamaskScriptInjector,
            metamaskWeb3JavaScriptInterface = metamaskWeb3JavaScriptInterface
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
    private val cardCreationInterceptor: WebViewCardCreationInterceptor,
    private val metamaskScriptInjector: MetamaskScriptInjector,
    private val metamaskWeb3JavaScriptInterface: MetamaskWeb3JavaScriptInterface,
    private val setupConfig: CardSetupConfig,
    private val widgetId: String,
    private val widgetSecret: String
) {

    private val permissionsAsker = permissionsAskerFactory.create(fragment)

    private val webViewClient = object : WebViewClient() {

        private var jsScriptWasCalled = false

        init {
            cardCreationInterceptor.runPolling(coroutineScope)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            metamaskScriptInjector.injectForPage(webView)
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (!jsScriptWasCalled) {
                jsScriptWasCalled = true
                //webView.evaluateJavascript(pageProvider.getJsScript(), null)
            }
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            cardCreationInterceptor.intercept(request)

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
        //webView.addJavascriptInterface(novaCardJsCallback, pageProvider.getCallbackName())

        metamaskScriptInjector.initialInject(webView)

        webView.webViewClient = webViewClient

        webView.webChromeClient = webChromeClient

        val address = setupConfig.refundAddress

        val signature = "$address$widgetSecret".encodeToByteArray()
            .sha512()
            .toHexString()

        val uri = Uri.parse("https://exchange.mercuryo.io").buildUpon()
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
            .appendQueryParameter("signature", signature)
            .build()

        webView.loadUrl(uri.toString())
    }
}
