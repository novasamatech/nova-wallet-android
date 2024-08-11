package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.model

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.hydra_dx_math.HydraDxMathConversions.fromBridgeResultToBalance
import io.novafoundation.nova.hydra_dx_math.xyk.HYKSwapMathBridge
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

class XYKPools(
    val fees: XYKFees,
    val pools: List<XYKPool>
) {

    fun quote(
        poolAddress: AccountId,
        assetIdIn: HydraDxAssetId,
        assetIdOut: HydraDxAssetId,
        amount: Balance,
        direction: SwapDirection
    ): Balance? {
        val relevantPool = pools.first { it.address.contentEquals(poolAddress) }

        return relevantPool.quote(assetIdIn, assetIdOut, amount, direction, fees)
    }
}

class XYKPool(
    val address: AccountId,
    val firstAsset: XYKPoolAsset,
    val secondAsset: XYKPoolAsset,
) {

    fun getAsset(assetId: HydraDxAssetId): XYKPoolAsset {
        return when {
            firstAsset.id == assetId -> firstAsset
            secondAsset.id == assetId -> secondAsset
            else -> error("Unknown asset for the pool")
        }
    }
}

class XYKPoolAsset(
    val balance: Balance,
    val id: HydraDxAssetId,
)

fun XYKPool.quote(
    assetIdIn: HydraDxAssetId,
    assetIdOut: HydraDxAssetId,
    amount: Balance,
    direction: SwapDirection,
    fees: XYKFees
): Balance? {
    return when (direction) {
        SwapDirection.SPECIFIED_IN -> calculateOutGivenIn(assetIdIn, assetIdOut, amount, fees)
        SwapDirection.SPECIFIED_OUT -> calculateInGivenOut(assetIdIn, assetIdOut, amount, fees)
    }
}

private fun XYKPool.calculateOutGivenIn(
    assetIdIn: HydraDxAssetId,
    assetIdOut: HydraDxAssetId,
    amountIn: Balance,
    feesConfig: XYKFees
): Balance? {
    val assetIn = getAsset(assetIdIn)
    val assetOut = getAsset(assetIdOut)

    val amountOut = HYKSwapMathBridge.calculate_out_given_in(
        assetIn.balance.toString(),
        assetOut.balance.toString(),
        amountIn.toString()
    ).fromBridgeResultToBalance() ?: return null

    val fees = feesConfig.feeFrom(amountOut) ?: return null

    return (amountOut - fees).atLeastZero()
}

private fun XYKPool.calculateInGivenOut(
    assetIdIn: HydraDxAssetId,
    assetIdOut: HydraDxAssetId,
    amountOut: Balance,
    feesConfig: XYKFees,
): Balance? {
    val assetIn = getAsset(assetIdIn)
    val assetOut = getAsset(assetIdOut)

    val amountIn = HYKSwapMathBridge.calculate_in_given_out(
        assetIn.balance.toString(),
        assetOut.balance.toString(),
        amountOut.toString()
    ).fromBridgeResultToBalance() ?: return null

    val fees = feesConfig.feeFrom(amountIn) ?: return null

    return amountIn + fees
}

private fun XYKFees.feeFrom(amount: BigInteger): Balance? {
    return HYKSwapMathBridge.calculate_pool_trade_fee(amount.toString(), nominator.toString(), denominator.toString())
        .fromBridgeResultToBalance()
}
