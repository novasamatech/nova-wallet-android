package io.novafoundation.nova.feature_swap_impl.presentation.confirmation.model

import io.novafoundation.nova.feature_swap_api.presentation.view.SwapAssetsView

class SwapConfirmationDetailsModel(
    val assets: SwapAssetsView.Model,
    val rate: String,
    val priceDifference: CharSequence?,
    val slippage: String
)
