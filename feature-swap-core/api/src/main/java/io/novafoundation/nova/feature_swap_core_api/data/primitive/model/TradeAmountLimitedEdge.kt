package io.novafoundation.nova.feature_swap_core_api.data.primitive.model

import java.math.BigInteger

/**
 * An edge whose underlying pool limits the size of a single trade, e.g. Hydration's XYK pools
 * that reject trades consuming more than `reserve / XYK.MaxInRatio` of the pool
 *
 * The limits are expressed against the quoted amount of the edge:
 * [minAllowedAmountIn] and [maxAllowedAmountIn] bound the input amount for [SwapDirection.SPECIFIED_IN],
 * [minAllowedAmountOut] and [maxAllowedAmountOut] bound the output amount for [SwapDirection.SPECIFIED_OUT].
 * Boundaries are inclusive and expressed in the corresponding asset's base units.
 *
 * `null` means there is no applicable bound, not that loading failed. Implementations must propagate
 * failures when a required runtime constant or pool state is unavailable.
 */
interface TradeAmountLimitedEdge {

    suspend fun minAllowedAmountIn(): BigInteger? = null

    suspend fun minAllowedAmountOut(): BigInteger? = null

    suspend fun maxAllowedAmountIn(): BigInteger? = null

    suspend fun maxAllowedAmountOut(): BigInteger? = null
}
