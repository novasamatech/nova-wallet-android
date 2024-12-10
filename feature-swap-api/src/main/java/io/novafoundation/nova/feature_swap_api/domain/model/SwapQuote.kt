package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedPath
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

data class SwapQuote(
    val amountIn: ChainAssetWithAmount,
    val amountOut: ChainAssetWithAmount,
    val priceImpact: Fraction,
    val quotedPath: QuotedPath<SwapGraphEdge>,
    val executionEstimate: SwapExecutionEstimate,
    val direction: SwapDirection,
) {

    val assetIn: Chain.Asset
        get() = amountIn.chainAsset

    val assetOut: Chain.Asset
        get() = amountOut.chainAsset

    val planksIn: Balance
        get() = amountIn.amount

    val planksOut: Balance
        get() = amountOut.amount
}

fun SwapQuote.swapRate(): BigDecimal {
    return amountIn rateAgainst amountOut
}

infix fun ChainAssetWithAmount.rateAgainst(assetOut: ChainAssetWithAmount): BigDecimal {
    if (amount == Balance.ZERO) return BigDecimal.ZERO

    val amountIn = chainAsset.amountFromPlanks(amount)
    val amountOut = assetOut.chainAsset.amountFromPlanks(assetOut.amount)

    return amountOut / amountIn
}
