package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
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
    val slippage: Fraction,
    val executionPath: Path<SegmentExecuteArgs>,
    val direction: SwapDirection,
    val firstSegmentFees: FeePaymentCurrency
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

val SwapLimit.estimatedAmountIn: Balance
    get() = when(this) {
        is SwapLimit.SpecifiedIn -> amountIn
        is SwapLimit.SpecifiedOut -> amountInQuote
    }

val SwapLimit.estimatedAmountOut: Balance
    get() = when(this) {
        is SwapLimit.SpecifiedIn -> amountOutQuote
        is SwapLimit.SpecifiedOut -> amountOut
    }

/**
 * Adjusts SwapLimit to the [newAmountIn] based on the quoted swap rate
 * This is only suitable for small changes amount in, as it implicitly assumes the swap rate stays the same
 */
fun SwapLimit.replaceAmountIn(newAmountIn: Balance, shouldReplaceBuyWithSell: Boolean): SwapLimit {
    return when (this) {
        is SwapLimit.SpecifiedIn -> updateInAmount(newAmountIn)
        is SwapLimit.SpecifiedOut -> {
            if (shouldReplaceBuyWithSell) {
                updateInAmountChangingToSell(newAmountIn)
            } else {
                updateInAmount(newAmountIn)
            }
        }
    }
}

private fun SwapLimit.SpecifiedOut.updateInAmountChangingToSell(newAmountIn: Balance): SwapLimit {
    val slippage = slippage()

    val inferredQuotedBalance = replacedInQuoteAmount(newAmountIn, amountOut)

    return SpecifiedIn(amount = newAmountIn, slippage, quotedBalance = inferredQuotedBalance)
}

private fun SwapLimit.SpecifiedOut.slippage(): Fraction {
    if (amountInQuote.isZero) return Fraction.ZERO

    val slippageAsFraction = (amountInMax.divideToDecimal(amountInQuote) - BigDecimal.ONE).atLeastZero()
    return slippageAsFraction.fractions
}

private fun SwapLimit.SpecifiedIn.replaceInMultiplier(amount: Balance): BigDecimal {
    return amount.divideToDecimal(amountIn)
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
    return amount.divideToDecimal(amountInQuote)
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

fun SwapQuote.toExecuteArgs(slippage: Fraction, firstSegmentFees: FeePaymentCurrency): SwapFeeArgs {
    return SwapFeeArgs(
        assetIn = amountIn.chainAsset,
        slippage = slippage,
        direction = quotedPath.direction,
        executionPath = quotedPath.path.map { quotedSwapEdge -> SegmentExecuteArgs(quotedSwapEdge) },
        firstSegmentFees = firstSegmentFees
    )
}

fun SwapLimit(direction: SwapDirection, amount: Balance, slippage: Fraction, quotedBalance: Balance): SwapLimit {
    return when (direction) {
        SwapDirection.SPECIFIED_IN -> SpecifiedIn(amount, slippage, quotedBalance)
        SwapDirection.SPECIFIED_OUT -> SpecifiedOut(amount, slippage, quotedBalance)
    }
}

private fun SpecifiedIn(amount: Balance, slippage: Fraction, quotedBalance: Balance): SwapLimit.SpecifiedIn {
    val lessAmountCoefficient = BigDecimal.ONE - slippage.inFraction.toBigDecimal()
    val amountOutMin = quotedBalance.toBigDecimal() * lessAmountCoefficient

    return SwapLimit.SpecifiedIn(
        amountIn = amount,
        amountOutQuote = quotedBalance,
        amountOutMin = amountOutMin.toBigInteger()
    )
}

private fun SpecifiedOut(amount: Balance, slippage: Fraction, quotedBalance: Balance): SwapLimit.SpecifiedOut {
    val moreAmountCoefficient = BigDecimal.ONE + slippage.inFraction.toBigDecimal()
    val amountInMax = quotedBalance.toBigDecimal() * moreAmountCoefficient

    return SwapLimit.SpecifiedOut(
        amountOut = amount,
        amountInQuote = quotedBalance,
        amountInMax = amountInMax.toBigInteger()
    )
}
