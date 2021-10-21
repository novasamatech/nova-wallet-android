package io.novafoundation.nova.feature_wallet_impl.data.buyToken

import android.content.Context
import io.novafoundation.nova.feature_wallet_api.domain.model.BuyTokenRegistry

interface ExternalProvider : BuyTokenRegistry.Provider<BuyTokenRegistry.Integrator<Context>> {

    companion object {
        const val REDIRECT_URL_BASE = "fearless://buy-success"
    }
}
