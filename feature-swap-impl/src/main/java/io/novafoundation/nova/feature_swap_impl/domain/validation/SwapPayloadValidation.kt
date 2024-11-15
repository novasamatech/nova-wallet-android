package io.novafoundation.nova.feature_swap_impl.domain.validation

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.createAggregated
import io.novafoundation.nova.feature_swap_impl.presentation.common.state.SwapState
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount

data class SwapValidationPayload(
    val fee: SwapFee,
    val swapQuote: SwapQuote,
    val slippage: Fraction
) {

    val amountIn: ChainAssetWithAmount = swapQuote.amountIn

    val amountOut: ChainAssetWithAmount = swapQuote.amountOut
}

fun SwapValidationPayload.estimatedSwapLimit(): SwapLimit {
    val firstLimit = fee.segments.first().operation.estimatedSwapLimit
    val lastLimit = fee.segments.last().operation.estimatedSwapLimit

    return SwapLimit.createAggregated(firstLimit, lastLimit)
}

fun SwapValidationPayload.toSwapState(): SwapState {
    return SwapState(swapQuote, fee, slippage)
}
