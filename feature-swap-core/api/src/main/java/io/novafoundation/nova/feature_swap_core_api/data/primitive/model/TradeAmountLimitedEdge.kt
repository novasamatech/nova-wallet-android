package io.novafoundation.nova.feature_swap_core_api.data.primitive.model

import java.math.BigInteger

/**
 * An edge whose underlying pool limits the size of a single trade, e.g. Hydration's XYK pools
 * that reject trades consuming more than `reserve / XYK.MaxInRatio` of the pool
 *
 * The limits are expressed against the quoted amount of the edge:
 * [maxAllowedAmountIn] bounds the input amount when quoting with [SwapDirection.SPECIFIED_IN],
 * [maxAllowedAmountOut] bounds the output amount when quoting with [SwapDirection.SPECIFIED_OUT]
 *
 * Both default to `null`, meaning the edge does not limit the trade size
 */
interface TradeAmountLimitedEdge {

    suspend fun maxAllowedAmountIn(): BigInteger? = null

    suspend fun maxAllowedAmountOut(): BigInteger? = null
}
