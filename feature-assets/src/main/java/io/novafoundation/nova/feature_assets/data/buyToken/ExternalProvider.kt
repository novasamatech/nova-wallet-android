package io.novafoundation.nova.feature_assets.data.buyToken

import android.content.Context

interface ExternalProvider : BuyTokenRegistry.Provider<ExternalProvider.Integrator> {

    companion object {
        const val REDIRECT_URL_BASE = "nova://buy-success"
    }

    interface Integrator : BuyTokenRegistry.Integrator<Context>
}
