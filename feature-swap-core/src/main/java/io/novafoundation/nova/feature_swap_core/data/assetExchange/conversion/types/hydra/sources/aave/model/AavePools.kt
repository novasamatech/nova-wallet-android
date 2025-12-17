package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave.model

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import java.math.BigInteger

data class AavePools(
    val pools: List<AavePool>
) {

    fun quote(
        assetIdIn: HydraDxAssetId,
        assetIdOut: HydraDxAssetId,
        amount: BigInteger,
        direction: SwapDirection
    ): BalanceOf? {
        val pool = findPool(assetIdIn, assetIdOut) ?: return null

        return pool.quote(assetIdOut, amount, direction)
    }

    private fun findPool(assetIdIn: HydraDxAssetId, assetIdOut: HydraDxAssetId): AavePool? {
        return pools.find { it.canHandleTrade(assetIdIn, assetIdOut) }
    }
}

data class AavePool(
    val reserve: HydraDxAssetId,
    val atoken: HydraDxAssetId,
    val liqudityIn: BalanceOf,
    val liquidityOut: BalanceOf
) {

    fun canHandleTrade(assetIdIn: HydraDxAssetId, assetIdOut: HydraDxAssetId): Boolean {
        return findPoolTokenLiquidity(assetIdIn) != null && findPoolTokenLiquidity(assetIdOut) != null
    }

    fun quote(
        assetIdOut: HydraDxAssetId,
        amount: BigInteger,
        direction: SwapDirection
    ): BalanceOf? {
        return when (direction) {
            SwapDirection.SPECIFIED_IN -> calculateOutGivenIn(assetIdOut, amount)
            SwapDirection.SPECIFIED_OUT -> calculateInGivenOut(assetIdOut, amount)
        }
    }

    // Here and in calculateInGivenOut we always validate amount out (either specified or calculated) against
    // assetIdOut liquidity since that's the asset that will be removed from the pool
    private fun calculateOutGivenIn(
        assetIdOut: HydraDxAssetId,
        amountIn: BigInteger,
    ): BalanceOf? {
        val calculatedOut = amountIn
        val liquidityOut = findPoolTokenLiquidity(assetIdOut) ?: return null

        return calculatedOut.takeIf { calculatedOut <= liquidityOut }
    }

    private fun calculateInGivenOut(
        assetIdOut: HydraDxAssetId,
        amountOut: BigInteger,
    ): BalanceOf? {
        val calculatedIn = amountOut
        val liquidityOut = findPoolTokenLiquidity(assetIdOut) ?: return null

        return calculatedIn.takeIf { amountOut <= liquidityOut }
    }

    private fun findPoolTokenLiquidity(assetId: HydraDxAssetId): BalanceOf? {
        return when (assetId) {
            reserve -> liqudityIn
            atoken -> liquidityOut
            else -> null
        }
    }
}
