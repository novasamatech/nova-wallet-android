package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.fraction
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

data class SwapQuoteArgs(
    val tokenIn: Token,
    val tokenOut: Token,
    val amount: Balance,
    val swapDirection: SwapDirection,
    val slippage: Percent,
)

class SwapExecuteArgs(
    val assetIn: Chain.Asset,
    val assetOut: Chain.Asset,
    val customFeeAsset: Chain.Asset?,
    val swapLimit: SwapLimit,
    val nativeAsset: Asset,
)

val SwapExecuteArgs.feeAsset: Chain.Asset
    get() = customFeeAsset ?: assetIn

sealed class SwapLimit(val expectedAmountIn: Balance, val expectedAmountOut: Balance) {

    class SpecifiedIn(
        expectedAmountIn: Balance,
        expectedAmountOut: Balance,
        val amountOutMin: Balance
    ) : SwapLimit(expectedAmountIn, expectedAmountOut)

    class SpecifiedOut(
        expectedAmountIn: Balance,
        expectedAmountOut: Balance,
        val amountInMax: Balance
    ) : SwapLimit(expectedAmountIn, expectedAmountOut)
}

fun SwapQuoteArgs.toExecuteArgs(quotedBalance: Balance, customFeeAsset: Chain.Asset?, nativeAsset: Asset): SwapExecuteArgs {
    return SwapExecuteArgs(
        assetIn = tokenIn.configuration,
        assetOut = tokenOut.configuration,
        swapLimit = swapLimits(quotedBalance),
        customFeeAsset = customFeeAsset,
        nativeAsset = nativeAsset
    )
}

fun SwapQuoteArgs.swapLimits(quotedBalance: Balance): SwapLimit {
    return when (swapDirection) {
        SwapDirection.SPECIFIED_IN -> SpecifiedIn(amount, slippage, quotedBalance)
        SwapDirection.SPECIFIED_OUT -> SpecifiedOut(amount, slippage, quotedBalance)
    }
}

@Suppress("FunctionName")
private fun SpecifiedIn(amount: Balance, slippage: Percent, quotedBalance: Balance): SwapLimit.SpecifiedIn {
    val lessAmountCoefficient = BigDecimal.ONE - slippage.fraction
    val amountOutMin = quotedBalance.toBigDecimal() * lessAmountCoefficient

    return SwapLimit.SpecifiedIn(
        expectedAmountIn = amount,
        expectedAmountOut = quotedBalance,
        amountOutMin = amountOutMin.toBigInteger()
    )
}

@Suppress("FunctionName")
private fun SpecifiedOut(amount: Balance, slippage: Percent, quotedBalance: Balance): SwapLimit.SpecifiedOut {
    val moreAmountCoefficient = BigDecimal.ONE + slippage.fraction
    val amountInMax = quotedBalance.toBigDecimal() * moreAmountCoefficient

    return SwapLimit.SpecifiedOut(
        expectedAmountIn = quotedBalance,
        expectedAmountOut = amount,
        amountInMax = amountInMax.toBigInteger()
    )
}
