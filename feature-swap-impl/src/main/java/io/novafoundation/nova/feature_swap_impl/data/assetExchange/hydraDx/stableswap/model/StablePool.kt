package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.hydra_dx_math.HydraDxMathConversions.fromBridgeResultToBalance
import io.novafoundation.nova.hydra_dx_math.stableswap.StableSwapMathBridge
import java.math.BigInteger

class StablePool(
    val sharedAsset: StablePoolAsset,
    sharedAssetIssuance: Balance,
    val assets: List<StablePoolAsset>,
    val initialAmplification: BigInteger,
    val finalAmplification: BigInteger,
    val initialBlock: BigInteger,
    val finalBlock: BigInteger,
    val currentBlock: BlockNumber,
    fee: Perbill,
    val gson: Gson,
) {

    val sharedAssetIssuance = sharedAssetIssuance.toString()
    val fee: String = fee.value.toBigDecimal().toPlainString()

    val reserves: String by lazy(LazyThreadSafetyMode.NONE) {
        val reservesInput = assets.map { ReservesInput(it.balance.toString(), it.id.toInt(), it.decimals) }
        gson.toJson(reservesInput)
    }

    val amplification by lazy(LazyThreadSafetyMode.NONE) {
        calculateAmplification()
    }

    private fun calculateAmplification(): String {
        return StableSwapMathBridge.calculate_amplification(
            initialAmplification.toString(),
            finalAmplification.toString(),
            initialBlock.toString(),
            finalBlock.toString(),
            currentBlock.toString()
        )
    }
}

class StablePoolAsset(
    val balance: Balance,
    val id: HydraDxAssetId,
    val decimals: Int
)

fun StablePool.quote(
    assetIdIn: HydraDxAssetId,
    assetIdOut: HydraDxAssetId,
    amount: Balance,
    direction: SwapDirection
): Balance? {
    return when (direction) {
        SwapDirection.SPECIFIED_IN -> calculateOutGivenIn(assetIdIn, assetIdOut, amount)
        SwapDirection.SPECIFIED_OUT -> calculateInGivenOut(assetIdIn, assetIdOut, amount)
    }
}

fun StablePool.calculateOutGivenIn(
    assetIn: HydraDxAssetId,
    assetOut: HydraDxAssetId,
    amountIn: Balance,
): Balance? {
    return when {
        assetIn == sharedAsset.id -> calculateWithdrawOneAsset(assetOut, amountIn)
        assetOut == sharedAsset.id -> calculateShares(assetIn, amountIn)
        else -> calculateOut(assetIn, assetOut, amountIn)
    }
}

fun StablePool.calculateInGivenOut(
    assetIn: HydraDxAssetId,
    assetOut: HydraDxAssetId,
    amountOut: Balance,
): Balance? {
    return when {
        assetOut == sharedAsset.id -> calculateAddOneAsset(assetIn, amountOut)
        assetIn == sharedAsset.id -> calculateSharesForAmount(assetOut, amountOut)
        else -> calculateIn(assetIn, assetOut, amountOut)
    }
}

private fun StablePool.calculateAddOneAsset(
    assetIn: HydraDxAssetId,
    amountOut: Balance,
): Balance? {
    return StableSwapMathBridge.calculate_add_one_asset(
        reserves,
        amountOut.toString(),
        assetIn.toInt(),
        amplification,
        sharedAssetIssuance,
        fee
    ).fromBridgeResultToBalance()
}

private fun StablePool.calculateSharesForAmount(
    assetOut: HydraDxAssetId,
    amountOut: Balance,
): Balance? {
    return StableSwapMathBridge.calculate_shares_for_amount(
        reserves,
        assetOut.toInt(),
        amountOut.toString(),
        amplification,
        sharedAssetIssuance,
        fee
    ).fromBridgeResultToBalance()
}

private fun StablePool.calculateIn(
    assetIn: HydraDxAssetId,
    assetOut: HydraDxAssetId,
    amountOut: Balance,
): Balance? {
    return StableSwapMathBridge.calculate_in_given_out(
        reserves,
        assetIn.toInt(),
        assetOut.toInt(),
        amountOut.toString(),
        amplification,
        fee
    ).fromBridgeResultToBalance()
}

private fun StablePool.calculateWithdrawOneAsset(
    assetOut: HydraDxAssetId,
    amountIn: Balance,
): Balance? {
    return StableSwapMathBridge.calculate_liquidity_out_one_asset(
        reserves,
        amountIn.toString(),
        assetOut.toInt(),
        amplification,
        sharedAssetIssuance,
        fee
    ).fromBridgeResultToBalance()
}

private fun StablePool.calculateShares(
    assetIn: HydraDxAssetId,
    amountIn: Balance,
): Balance? {
    val assets = listOf(SharesAssetInput(assetIn.toInt(), amountIn.toString()))
    val assetsJson = gson.toJson(assets)

    return StableSwapMathBridge.calculate_shares(
        reserves,
        assetsJson,
        amplification,
        sharedAssetIssuance,
        fee
    ).fromBridgeResultToBalance()
}

private fun StablePool.calculateOut(
    assetIn: HydraDxAssetId,
    assetOut: HydraDxAssetId,
    amountIn: Balance,
): Balance? {
    return StableSwapMathBridge.calculate_out_given_in(
        this.reserves,
        assetIn.toInt(),
        assetOut.toInt(),
        amountIn.toString(),
        amplification,
        fee
    ).fromBridgeResultToBalance()
}

private class SharesAssetInput(@SerializedName("asset_id") val assetId: Int, val amount: String)

private class ReservesInput(
    val amount: String,
    @SerializedName("asset_id")
    val id: Int,
    val decimals: Int
)
