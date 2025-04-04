package io.novafoundation.nova.feature_buy_api.domain.providers

import android.webkit.WebView
import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry

interface InternalProvider : TradeTokenRegistry.Provider<InternalProvider.Integrator> {

    interface Integrator : TradeTokenRegistry.Integrator<WebView>
}
