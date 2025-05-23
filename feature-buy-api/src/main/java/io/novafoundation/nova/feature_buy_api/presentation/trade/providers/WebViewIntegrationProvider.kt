package io.novafoundation.nova.feature_buy_api.presentation.trade.providers

import android.webkit.WebView
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry

interface WebViewIntegrationProvider : TradeTokenRegistry.Provider<WebViewIntegrationProvider.Integrator> {

    interface Integrator : TradeTokenRegistry.Integrator<WebView>
}
