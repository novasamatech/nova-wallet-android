package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

class SwapQuote(
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
    val networkFee: Fee
)
