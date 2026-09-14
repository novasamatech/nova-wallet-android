package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.model

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.hydra_dx_math.HydraDxMathConversions.fromBridgeResultToBalance
import io.novafoundation.nova.hydra_dx_math.xyk.HYKSwapMathBridge
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

class XYKPools(
    val fees: XYKFees,
    val maxInRatio: BigInteger?,
    val maxOutRatio: BigInteger?,
    val pools: List<XYKPool>
) {

    fun quote(
        poolAddress: AccountId,
        assetIdIn: HydraDxAssetId,
        assetIdOut: HydraDxAssetId,
        amount: BigInteger,
        direction: SwapDirection
    ): BigInteger? {
        val relevantPool = pools.first { it.address.contentEquals(poolAddress) }

        return relevantPool.quote(assetIdIn, assetIdOut, amount, direction, fees)
    }

    fun maxAllowedAmountIn(
        poolAddress: AccountId,
        assetIdIn: HydraDxAssetId,
        assetIdOut: HydraDxAssetId
    ): BigInteger? {
        val relevantPool = pools.firstOrNull { it.address.contentEquals(poolAddress) } ?: return null

        return relevantPool.maxAllowedAmountIn(assetIdIn, assetIdOut, maxInRatio, maxOutRatio)
    }

    fun maxAllowedAmountOut(
        poolAddress: AccountId,
        assetIdIn: HydraDxAssetId,
        assetIdOut: HydraDxAssetId
    ): BigInteger? {
        val relevantPool = pools.firstOrNull { it.address.contentEquals(poolAddress) } ?: return null

        return relevantPool.maxAllowedAmountOut(assetIdIn, assetIdOut, maxInRatio, maxOutRatio)
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
    val balance: BigInteger,
    val id: HydraDxAssetId,
)

fun XYKPool.quote(
    assetIdIn: HydraDxAssetId,
    assetIdOut: HydraDxAssetId,
    amount: BigInteger,
    direction: SwapDirection,
    fees: XYKFees
): BigInteger? {
    return when (direction) {
        SwapDirection.SPECIFIED_IN -> calculateOutGivenIn(assetIdIn, assetIdOut, amount, fees)
        SwapDirection.SPECIFIED_OUT -> calculateInGivenOut(assetIdIn, assetIdOut, amount, fees)
    }
}

private fun XYKPool.calculateOutGivenIn(
    assetIdIn: HydraDxAssetId,
    assetIdOut: HydraDxAssetId,
    amountIn: BigInteger,
    feesConfig: XYKFees
): BigInteger? {
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
    amountOut: BigInteger,
    feesConfig: XYKFees,
): BigInteger? {
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

private fun XYKFees.feeFrom(amount: BigInteger): BigInteger? {
    return HYKSwapMathBridge.calculate_pool_trade_fee(amount.toString(), nominator.toString(), denominator.toString())
        .fromBridgeResultToBalance()
}

/**
 * Mirrors the trade size limits from XYK pallet's `validate_sell`:
 * `amount_in <= reserve_in / MaxInRatio` and the pre-fee `amount_out <= reserve_out / MaxOutRatio`
 *
 * The second check is folded into a bound on `amount_in` via the constant-product formula:
 * `amount_out = reserve_out * amount_in / (reserve_in + amount_in) <= maxOut` <=> `amount_in <= maxOut * reserve_in / (reserve_out - maxOut)`
 *
 * `null` means the pool does not limit the trade size (constants are absent in the runtime)
 */
fun XYKPool.maxAllowedAmountIn(
    assetIdIn: HydraDxAssetId,
    assetIdOut: HydraDxAssetId,
    maxInRatio: BigInteger?,
    maxOutRatio: BigInteger?
): BigInteger? {
    val reserveIn = getAsset(assetIdIn).balance
    val reserveOut = getAsset(assetIdOut).balance

    val byInRatio = maxInRatio?.takeIf { it.isPositive() }?.let { reserveIn / it }

    val byOutRatio = maxOutRatio?.takeIf { it.isPositive() }?.let { ratio ->
        val maxOut = reserveOut / ratio
        val denominator = reserveOut - maxOut
        if (denominator.isPositive()) maxOut * reserveIn / denominator else null
    }

    return listOfNotNull(byInRatio, byOutRatio).minOrNull()
}

/**
 * Mirrors the trade size limits from XYK pallet's `validate_buy`:
 * `amount_out <= reserve_out / MaxOutRatio` and the pre-fee `amount_in <= reserve_in / MaxInRatio`
 *
 * The second check is folded into a bound on `amount_out` via the constant-product formula:
 * `amount_in = reserve_in * amount_out / (reserve_out - amount_out) <= maxIn` <=> `amount_out <= maxIn * reserve_out / (reserve_in + maxIn)`
 *
 * With the mainnet constants (MaxInRatio = MaxOutRatio = 3) the MaxInRatio bound is the stricter one:
 * the effective cap is `reserve_out / 4`, not `reserve_out / 3`
 *
 * `null` means the pool does not limit the trade size (constants are absent in the runtime)
 */
fun XYKPool.maxAllowedAmountOut(
    assetIdIn: HydraDxAssetId,
    assetIdOut: HydraDxAssetId,
    maxInRatio: BigInteger?,
    maxOutRatio: BigInteger?
): BigInteger? {
    val reserveIn = getAsset(assetIdIn).balance
    val reserveOut = getAsset(assetIdOut).balance

    val byOutRatio = maxOutRatio?.takeIf { it.isPositive() }?.let { reserveOut / it }

    val byInRatio = maxInRatio?.takeIf { it.isPositive() }?.let { ratio ->
        val maxIn = reserveIn / ratio
        val denominator = reserveIn + maxIn
        if (denominator.isPositive()) maxIn * reserveOut / denominator else null
    }

    return listOfNotNull(byOutRatio, byInRatio).minOrNull()
}
