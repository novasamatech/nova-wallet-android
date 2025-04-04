package io.novafoundation.nova.feature_buy_api.domain.providers

import android.webkit.WebView
import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry

interface WebViewIntegrationProvider : TradeTokenRegistry.Provider<WebViewIntegrationProvider.Integrator> {

    interface Integrator : TradeTokenRegistry.Integrator<WebView>
}
