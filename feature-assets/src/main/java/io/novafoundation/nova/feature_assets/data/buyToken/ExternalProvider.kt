package io.novafoundation.nova.feature_assets.data.buyToken

import android.content.Context

interface ExternalProvider : BuyTokenRegistry.Provider<BuyTokenRegistry.Integrator<Context>> {

    companion object {
        const val REDIRECT_URL_BASE = "fearless://buy-success"
    }
}
