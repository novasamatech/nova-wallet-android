package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

data class SwapQuote(
    val assetIn: Chain.Asset,
    val assetOut: Chain.Asset,
    val planksIn: Balance,
    val planksOut: Balance,
    val direction: SwapDirection,
    val priceImpact: Percent,
) {

    init {
        require(assetIn.chainId == assetOut.chainId) {
            "Cross-chain swaps are not yet implemented"
        }
    }
}

val SwapQuote.editedBalance: Balance
    get() = when (direction) {
        SwapDirection.SPECIFIED_IN -> planksIn
        SwapDirection.SPECIFIED_OUT -> planksOut
    }

val SwapQuote.quotedBalance: Balance
    get() = when (direction) {
        SwapDirection.SPECIFIED_IN -> planksOut
        SwapDirection.SPECIFIED_OUT -> planksIn
    }

fun SwapQuote.swapRate(): BigDecimal {
    if (planksIn == Balance.ZERO) return BigDecimal.ZERO

    val amountIn = assetIn.amountFromPlanks(planksIn)
    val amountOut = assetOut.amountFromPlanks(planksOut)

    return amountOut / amountIn
}

class SwapFee(
    override val networkFee: Fee,
    val minimumBalanceBuyIn: MinimumBalanceBuyIn,
) : GenericFee

val SwapFee.totalDeductedPlanks: Balance
    get() = networkFee.amount + minimumBalanceBuyIn.commissionAssetToSpendOnBuyIn

sealed class MinimumBalanceBuyIn {

    class NeedsToBuyMinimumBalance(
        val nativeAsset: Chain.Asset,
        val nativeMinimumBalance: Balance,
        val commissionAsset: Chain.Asset,
        val commissionAssetToSpendOnBuyIn: Balance
    ) : MinimumBalanceBuyIn()

    object NoBuyInNeeded : MinimumBalanceBuyIn()
}

val MinimumBalanceBuyIn.commissionAssetToSpendOnBuyIn: Balance
    get() = when (this) {
        is MinimumBalanceBuyIn.NeedsToBuyMinimumBalance -> commissionAssetToSpendOnBuyIn
        MinimumBalanceBuyIn.NoBuyInNeeded -> Balance.ZERO
    }

fun MinimumBalanceBuyIn.requireNativeAsset(): Chain.Asset {
    return when (this) {
        is MinimumBalanceBuyIn.NeedsToBuyMinimumBalance -> nativeAsset
        MinimumBalanceBuyIn.NoBuyInNeeded -> throw IllegalStateException("No buy-in needed")
    }
}

val MinimumBalanceBuyIn.nativeMinimumBalance: Balance
    get() = when (this) {
        is MinimumBalanceBuyIn.NeedsToBuyMinimumBalance -> nativeMinimumBalance
        MinimumBalanceBuyIn.NoBuyInNeeded -> Balance.ZERO
    }
