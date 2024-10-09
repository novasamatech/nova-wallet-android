package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.fraction
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

data class SwapQuoteArgs(
    val tokenIn: Token,
    val tokenOut: Token,
    val amount: Balance,
    val swapDirection: SwapDirection,
)

class SwapExecuteArgs(
    val slippage: Percent,
    val executionPath: Path<SegmentExecuteArgs>,
    val direction: SwapDirection,
    val firstSegmentFees: Chain.Asset
)

class SegmentExecuteArgs(
    val quotedSwapEdge: QuotedEdge<SwapGraphEdge>,
)

sealed class SwapLimit {

    class SpecifiedIn(
        val amountIn: Balance,
        val amountOutQuote: Balance,
        val amountOutMin: Balance
    ) : SwapLimit()

    class SpecifiedOut(
        val amountOut: Balance,
        val amountInQuote: Balance,
        val amountInMax: Balance
    ) : SwapLimit()
}

fun SwapQuote.toExecuteArgs(slippage: Percent, firstSegmentFees: Chain.Asset): SwapExecuteArgs {
    return SwapExecuteArgs(
        slippage = slippage,
        direction = quotedPath.direction,
        executionPath = quotedPath.path.map { quotedSwapEdge -> SegmentExecuteArgs(quotedSwapEdge) },
        firstSegmentFees = firstSegmentFees
    )
}

fun SwapLimit(direction: SwapDirection, amount: Balance, slippage: Percent, quotedBalance: Balance): SwapLimit {
    return when (direction) {
        SwapDirection.SPECIFIED_IN -> SpecifiedIn(amount, slippage, quotedBalance)
        SwapDirection.SPECIFIED_OUT -> SpecifiedOut(amount, slippage, quotedBalance)
    }
}

@Suppress("FunctionName")
private fun SpecifiedIn(amount: Balance, slippage: Percent, quotedBalance: Balance): SwapLimit.SpecifiedIn {
    val lessAmountCoefficient = BigDecimal.ONE - slippage.fraction
    val amountOutMin = quotedBalance.toBigDecimal() * lessAmountCoefficient

    return SwapLimit.SpecifiedIn(
        amountIn = amount,
        amountOutQuote = quotedBalance,
        amountOutMin = amountOutMin.toBigInteger()
    )
}

@Suppress("FunctionName")
private fun SpecifiedOut(amount: Balance, slippage: Percent, quotedBalance: Balance): SwapLimit.SpecifiedOut {
    val moreAmountCoefficient = BigDecimal.ONE + slippage.fraction
    val amountInMax = quotedBalance.toBigDecimal() * moreAmountCoefficient

    return SwapLimit.SpecifiedOut(
        amountOut = amount,
        amountInQuote = quotedBalance,
        amountInMax = amountInMax.toBigInteger()
    )
}
