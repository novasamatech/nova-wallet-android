package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import android.net.Uri
import android.webkit.WebView
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.utils.webView.BaseWebChromeClient
import io.novafoundation.nova.common.utils.webView.BaseWebChromeClientFactory
import io.novafoundation.nova.common.utils.webView.InterceptingWebViewClient
import io.novafoundation.nova.common.utils.webView.InterceptingWebViewClientFactory
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.model.CardSetupConfig
import io.novafoundation.nova.common.utils.webView.WebViewRequestInterceptor
import kotlinx.coroutines.CoroutineScope

class NovaCardWebViewControllerFactory(
    private val interceptingWebViewClientFactory: InterceptingWebViewClientFactory,
    private val novaCardWebChromeClientFactory: BaseWebChromeClientFactory,
    private val appLinksProvider: AppLinksProvider,
    private val widgetId: String
) {

    fun create(
        interceptors: List<WebViewRequestInterceptor>,
        setupConfig: CardSetupConfig,
        scope: CoroutineScope,
    ): NovaCardWebViewController {
        return NovaCardWebViewController(
            interceptingWebViewClient = interceptingWebViewClientFactory.create(interceptors),
            novaCardWebChromeClient = novaCardWebChromeClientFactory.create(scope),
            appLinksProvider = appLinksProvider,
            setupConfig = setupConfig,
            widgetId = widgetId
        )
    }
}

class NovaCardWebViewController(
    private val interceptingWebViewClient: InterceptingWebViewClient,
    private val novaCardWebChromeClient: BaseWebChromeClient,
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

        webView.webViewClient = interceptingWebViewClient
        webView.webChromeClient = novaCardWebChromeClient

        loadUrl(webView)
    }

    private fun loadUrl(webView: WebView) {
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
