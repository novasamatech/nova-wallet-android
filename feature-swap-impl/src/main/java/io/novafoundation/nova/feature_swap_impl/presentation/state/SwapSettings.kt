package io.novafoundation.nova.feature_swap_impl.presentation.state

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.domain.model.Slippage
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

data class SwapSettings(
    val assetIn: Chain.Asset? = null,
    val assetOut: Chain.Asset? = null,
    val feeAsset: Chain.Asset? = null,
    val amount: Balance? = null,
    val swapDirection: SwapDirection? = null,
    val slippage: Percent = Slippage.DEFAULT
)



fun SwapSettings.toExecuteArgs(): SwapExecuteArgs? {
    val swapLimits = getSwapLimit()
    return if (assetIn != null && assetOut != null && amount != null && swapDirection != null && swapLimits != null) {
        SwapExecuteArgs(assetIn, assetOut, feeAsset, swapLimits)
    } else {
        null
    }
}

fun SwapSettings.getSwapLimit(): SwapLimit? {
    if (amount == null || swapDirection == null) return null
    return when (swapDirection) {
        SwapDirection.SPECIFIED_IN -> SwapLimit.SpecifiedIn(amount, amount) //TODO: Provide valid out amount
        SwapDirection.SPECIFIED_OUT -> SwapLimit.SpecifiedOut(amount, amount) //TODO: Provide valid in amount
    }
}
