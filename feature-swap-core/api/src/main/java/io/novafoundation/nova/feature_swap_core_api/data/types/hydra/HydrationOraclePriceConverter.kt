package io.novafoundation.nova.feature_swap_core_api.data.types.hydra

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

/**
 * Converts a native-asset amount the same way the chain does when charging a fee in a non-native currency:
 * the EMA price over the route registered in `Router.Routes`.
 *
 * Deliberately not a swap quote - the chain never swaps the fee, and a best-path quote lands on whichever
 * pool is furthest from the market, which is exactly the pool a cheapest-route search selects.
 */
interface HydrationOraclePriceConverter {

    /**
     * @return null when the route or oracle entries are unavailable
     */
    suspend fun convertNativeAmount(nativeAmount: BalanceOf, conversionTarget: Chain.Asset): BalanceOf?
}
