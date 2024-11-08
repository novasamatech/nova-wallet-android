package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationFeeDisplayData.SwapFeeComponentDisplay
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicOperationFeeDisplayData.SwapFeeType

class AtomicOperationFeeDisplayData(
    val components: List<SwapFeeComponentDisplay>
) {

    class SwapFeeComponentDisplay(
        val fees: List<FeeBase>,
        val type: SwapFeeType
    ) {

        companion object;
    }

    enum class SwapFeeType {
        NETWORK, CROSS_CHAIN
    }
}

fun SwapFeeComponentDisplay.Companion.network(vararg fee: FeeBase): SwapFeeComponentDisplay {
    return SwapFeeComponentDisplay(fee.toList(), SwapFeeType.NETWORK)
}

fun SwapFeeComponentDisplay.Companion.crossChain(vararg fee: FeeBase): SwapFeeComponentDisplay {
    return SwapFeeComponentDisplay(fee.toList(), SwapFeeType.CROSS_CHAIN)
}
