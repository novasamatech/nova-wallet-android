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

open class SwapFeeArgs(
    val assetIn: Chain.Asset,
    val slippage: Percent,
    val executionPath: Path<SegmentExecuteArgs>,
    val direction: SwapDirection,
    val firstSegmentFees: Chain.Asset
)

class SegmentExecuteArgs(
    val quotedSwapEdge: QuotedEdge<SwapGraphEdge>,
)

sealed class SwapLimit {

    data class SpecifiedIn(
        val amountIn: Balance,
        val amountOutQuote: Balance,
        val amountOutMin: Balance
    ) : SwapLimit()

    data class SpecifiedOut(
        val amountOut: Balance,
        val amountInQuote: Balance,
        val amountInMax: Balance
    ) : SwapLimit()
}

/**
 * Adjusts SwapLimit to the [newAmountIn] based on the quoted swap rate
 * This is only suitable for small changes amount in, as it implicitly assumes the swap rate stays the same
 */
fun SwapLimit.replaceAmountIn(newAmountIn: Balance): SwapLimit {
    return when(this) {
        is SwapLimit.SpecifiedIn -> updateInAmount(newAmountIn)
        is SwapLimit.SpecifiedOut -> updateInAmount(newAmountIn)
    }
}

private fun SwapLimit.SpecifiedIn.replaceInMultiplier(amount: Balance): BigDecimal {
    val amountDecimal = amount.toBigDecimal()
    val amountInDecimal = amountIn.toBigDecimal()

    return amountDecimal / amountInDecimal
}

private fun SwapLimit.SpecifiedIn.replacingInAmount(newInAmount: Balance, replacingAmount: Balance): Balance {
    return (replaceInMultiplier(replacingAmount) * newInAmount.toBigDecimal()).toBigInteger()
}

private fun SwapLimit.SpecifiedIn.updateInAmount(newAmountIn: Balance): SwapLimit.SpecifiedIn {
    return SwapLimit.SpecifiedIn(
        amountIn = newAmountIn,
        amountOutQuote = replacingInAmount(newAmountIn, replacingAmount = amountOutQuote),
        amountOutMin = replacingInAmount(newAmountIn, replacingAmount = amountOutMin)
    )
}

private fun SwapLimit.SpecifiedOut.replaceInQuoteMultiplier(amount: Balance): BigDecimal {
    val amountDecimal = amount.toBigDecimal()
    val amountInQuoteDecimal = amountInQuote.toBigDecimal()

    return amountDecimal / amountInQuoteDecimal
}

private fun SwapLimit.SpecifiedOut.replacedInQuoteAmount(newInQuoteAmount: Balance, replacingAmount: Balance): Balance {
    return (replaceInQuoteMultiplier(replacingAmount) * newInQuoteAmount.toBigDecimal()).toBigInteger()
}

private fun SwapLimit.SpecifiedOut.updateInAmount(newAmountInQuote: Balance): SwapLimit.SpecifiedOut {
    return SwapLimit.SpecifiedOut(
        amountOut = replacedInQuoteAmount(newAmountInQuote, amountOut),
        amountInQuote = newAmountInQuote,
        amountInMax = replacedInQuoteAmount(newAmountInQuote, amountInMax)
    )
}

fun SwapQuote.toExecuteArgs(slippage: Percent, firstSegmentFees: Chain.Asset): SwapFeeArgs {
    return SwapFeeArgs(
        assetIn = amountIn.chainAsset,
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
