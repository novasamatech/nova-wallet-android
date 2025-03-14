package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import android.net.Uri
import android.webkit.WebView
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.model.CardSetupConfig
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.NovaCardInterceptor
import kotlinx.coroutines.CoroutineScope

class NovaCardWebViewControllerFactory(
    private val novaCardWebViewClientFactory: NovaCardWebViewClientFactory,
    private val novaCardWebChromeClientFactory: NovaCardWebChromeClientFactory,
    private val appLinksProvider: AppLinksProvider,
    private val widgetId: String
) {

    fun create(
        interceptors: List<NovaCardInterceptor>,
        setupConfig: CardSetupConfig,
        scope: CoroutineScope,
    ): NovaCardWebViewController {
        return NovaCardWebViewController(
            novaCardWebViewClient = novaCardWebViewClientFactory.create(interceptors),
            novaCardWebChromeClient = novaCardWebChromeClientFactory.create(scope),
            appLinksProvider = appLinksProvider,
            setupConfig = setupConfig,
            widgetId = widgetId
        )
    }
}

class NovaCardWebViewController(
    private val novaCardWebViewClient: NovaCardWebViewClient,
    private val novaCardWebChromeClient: NovaCardWebChromeClient,
    private val appLinksProvider: AppLinksProvider,
    private val setupConfig: CardSetupConfig,
    private val widgetId: String
) {

    fun setup(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            displayZoomControls = false
        }

        webView.webViewClient = novaCardWebViewClient
        webView.webChromeClient = novaCardWebChromeClient

        val uri = Uri.parse(appLinksProvider.novaCardWidgetUrl).buildUpon()
            .appendQueryParameter("widget_id", widgetId)
            .appendQueryParameter("type", "sell")
            .appendQueryParameter("currencies", setupConfig.spendToken.symbol.value)
            .appendQueryParameter("theme", "nova")
            .appendQueryParameter("show_spend_card_details", "true")
            .appendQueryParameter("hide_refund_address", "true")
            .appendQueryParameter("refund_address", setupConfig.refundAddress)
            .build()

        webView.loadUrl(uri.toString())
    }
}
