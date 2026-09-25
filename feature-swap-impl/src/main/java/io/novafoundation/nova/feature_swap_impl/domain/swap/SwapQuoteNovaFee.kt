package io.novafoundation.nova.feature_swap_impl.domain.swap

import io.novafoundation.nova.feature_swap_api.domain.model.NovaSwapCommission
import io.novafoundation.nova.feature_swap_api.domain.model.NovaFeeChargingSwapEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description.SwapRateDescriptionMode

/**
 * Edge-type detection is required: chain-id detection over-matches cross-chain transfer edges to/from
 * commission-enabled chains, which are not swaps.
 */
fun SwapQuote.includesNovaFee(): Boolean {
    return quotedPath.path.any { it.edge is NovaFeeChargingSwapEdge }
}

fun SwapQuote.swapRateDescriptionMode(): SwapRateDescriptionMode {
    return if (includesNovaFee()) {
        SwapRateDescriptionMode.IncludesFee(NovaSwapCommission.FEE_PERCENT_DISPLAY)
    } else {
        SwapRateDescriptionMode.Default
    }
}
