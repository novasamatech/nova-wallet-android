package io.novafoundation.nova.feature_swap_impl.presentation.confirmation.model

import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class SwapConfirmationDetailsModel(
    val assetInDetails: SwapAssetView.Model,
    val assetOutDetails: SwapAssetView.Model,
    val rate: String,
    val priceDifference: CharSequence?,
    val slippage: String,
    val networkFee: AmountModel
)
