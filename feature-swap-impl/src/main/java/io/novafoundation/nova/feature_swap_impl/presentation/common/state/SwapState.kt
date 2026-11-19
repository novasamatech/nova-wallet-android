package io.novafoundation.nova.feature_swap_impl.presentation.common.state

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapDustCheckFee

class SwapState(
    val quote: SwapQuote,
    val fee: SwapFee,
    val slippage: Fraction,
    val dustCheckFee: SwapDustCheckFee = SwapDustCheckFee.QuoteFee,
)
