package io.novafoundation.nova.feature_swap_impl.presentation.common.state

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote

class SwapState(
    val quote: SwapQuote,
    val fee: SwapFee,
    val slippage: Percent,
)
