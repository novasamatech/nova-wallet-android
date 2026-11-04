package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

/** The exact quoted execution inputs, retained with the operations estimated from them. */
data class SwapFeeQuoteIdentity(
    val assetIn: FullChainAssetId,
    val assetOut: FullChainAssetId,
    val displayedAmountIn: Balance,
    val displayedAmountOut: Balance,
    val direction: SwapDirection,
    val slippage: Fraction,
    val firstSegmentFees: FeePaymentCurrency,
    val path: List<Segment>,
) {

    data class Segment(
        val edgeType: String,
        val assetIn: FullChainAssetId,
        val assetOut: FullChainAssetId,
        val poolId: SwapPoolId?,
        val chargesNovaFee: Boolean,
        val quotedAmount: Balance,
        val quote: Balance,
    )
}

fun SwapFeeArgs.quoteIdentity(): SwapFeeQuoteIdentity {
    return SwapFeeQuoteIdentity(
        assetIn = assetIn.fullId,
        assetOut = displayedAmountOut.chainAsset.fullId,
        displayedAmountIn = displayedAmountIn.amount,
        displayedAmountOut = displayedAmountOut.amount,
        direction = direction,
        slippage = slippage,
        firstSegmentFees = firstSegmentFees,
        path = executionPath.map { segment ->
            val quotedEdge = segment.quotedSwapEdge
            val edge = quotedEdge.edge
            SwapFeeQuoteIdentity.Segment(
                edgeType = edge.javaClass.name,
                assetIn = edge.from,
                assetOut = edge.to,
                poolId = edge.poolId,
                chargesNovaFee = edge is NovaFeeChargingSwapEdge,
                quotedAmount = quotedEdge.quotedAmount,
                quote = quotedEdge.quote
            )
        }
    )
}

fun SwapFee.matchesQuote(quote: SwapQuote, slippage: Fraction, feePaymentCurrency: FeePaymentCurrency? = null): Boolean {
    val identity = quoteIdentity ?: return false
    if (quote.direction != quote.quotedPath.direction) return false
    val firstEdge = quote.quotedPath.path.firstOrNull()?.edge ?: return false
    val lastEdge = quote.quotedPath.path.last().edge
    if (quote.assetIn.fullId != firstEdge.from || quote.assetOut.fullId != lastEdge.to) return false

    val requestedIdentity = quote.toExecuteArgs(slippage, feePaymentCurrency ?: identity.firstSegmentFees).quoteIdentity()
    return identity == requestedIdentity
}
