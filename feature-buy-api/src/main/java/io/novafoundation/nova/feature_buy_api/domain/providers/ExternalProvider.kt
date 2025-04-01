package io.novafoundation.nova.feature_buy_api.domain.providers

import android.content.Context
import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry

interface ExternalProvider : TradeTokenRegistry.Provider<ExternalProvider.Integrator> {

    companion object {
        const val REDIRECT_URL_BASE = "nova://buy-success"
    }

    interface Integrator : TradeTokenRegistry.Integrator<Context>
}
