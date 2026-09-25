package io.novafoundation.nova.feature_swap_impl.presentation.execution.model

import io.novafoundation.nova.feature_wallet_api.presentation.view.swap.SwapAssetsView
import io.novafoundation.nova.feature_swap_impl.presentation.common.route.SwapRouteModel

class SwapExecutionDetailsModel(
    val assets: SwapAssetsView.Model,
    val rate: String,
    val priceDifference: CharSequence?,
    val slippage: String,
    val swapRouteModel: SwapRouteModel?,
)
