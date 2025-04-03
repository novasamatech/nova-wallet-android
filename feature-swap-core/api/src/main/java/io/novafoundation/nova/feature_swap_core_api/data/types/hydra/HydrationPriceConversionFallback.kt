package io.novafoundation.nova.feature_swap_core_api.data.types.hydra

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface HydrationPriceConversionFallback {

    suspend fun convertNativeAmount(
        amount: BalanceOf,
        conversionTarget: Chain.Asset
    ): BalanceOf
}
