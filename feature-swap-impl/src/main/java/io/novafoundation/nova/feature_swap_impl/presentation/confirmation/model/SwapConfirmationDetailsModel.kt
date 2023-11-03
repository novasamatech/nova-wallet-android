package io.novafoundation.nova.feature_swap_impl.presentation.confirmation.model

import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAssetView

class SwapConfirmationDetailsModel(
    val assetInDetails: SwapAssetView.Model,
    val assetOutDetails: SwapAssetView.Model,
    val rate: String,
    val priceDifference: CharSequence?,
    val slippage: String
)
