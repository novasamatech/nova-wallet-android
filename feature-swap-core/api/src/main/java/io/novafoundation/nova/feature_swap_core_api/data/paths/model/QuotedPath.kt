package io.novafoundation.nova.feature_swap_core_api.data.paths.model

import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import java.math.BigInteger

class QuotedPath<E>(
    val direction: SwapDirection,
    val path: Path<QuotedEdge<E>>,
    val roughFeeEstimation: PathRoughFeeEstimation,
) : Comparable<QuotedPath<E>> {

    private val amountOutAfterFees: BigInteger = lastSegmentQuote - roughFeeEstimation.inAssetOut
    private val amountInAfterFees: BigInteger = firstSegmentQuote + roughFeeEstimation.inAssetIn

    override fun compareTo(other: QuotedPath<E>): Int {
        return when (direction) {
            // When we want to sell a token, the bigger the quote - the better
            SwapDirection.SPECIFIED_IN -> (amountOutAfterFees - other.amountOutAfterFees).signum()
            // When we want to buy a token, the smaller the quote - the better
            SwapDirection.SPECIFIED_OUT -> (other.amountInAfterFees - amountInAfterFees).signum()
        }
    }
}

val QuotedPath<*>.quote: BigInteger
    get() = when(direction) {
        SwapDirection.SPECIFIED_IN -> lastSegmentQuote
        SwapDirection.SPECIFIED_OUT -> firstSegmentQuote
    }

val QuotedPath<*>.lastSegmentQuotedAmount: BigInteger
    get() = path.last().quotedAmount

val QuotedPath<*>.lastSegmentQuote: BigInteger
    get() = path.last().quote

val QuotedPath<*>.firstSegmentQuote: BigInteger
    get() = path.first().quote

val QuotedPath<*>.firstSegmentQuotedAmount: BigInteger
    get() = path.first().quotedAmount
