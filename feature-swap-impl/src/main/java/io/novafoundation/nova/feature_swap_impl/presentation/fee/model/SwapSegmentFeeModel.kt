package io.novafoundation.nova.feature_swap_impl.presentation.fee.model

import io.novafoundation.nova.feature_swap_impl.presentation.common.route.SwapRouteModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay

class SwapSegmentFeeModel(
    val operation: FeeOperationModel,
    val feeComponents: List<SwapComponentFeeModel>
) {

    class SwapComponentFeeModel(val label: String, val individualFees: List<FeeDisplay>)

    class FeeOperationModel(val label: String, val swapRoute: SwapRouteModel)
}
