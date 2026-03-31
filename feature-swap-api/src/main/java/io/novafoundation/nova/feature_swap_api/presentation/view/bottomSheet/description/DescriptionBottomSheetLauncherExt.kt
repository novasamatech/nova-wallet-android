package io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_swap_api.R

fun DescriptionBottomSheetLauncher.launchSwapRateDescription(
    resourceManager: ResourceManager,
    includesNovaFee: Boolean,
    feePercentDisplay: String
) {
    if (includesNovaFee) {
        val description = resourceManager.getString(R.string.swap_rate_includes_fee_description, feePercentDisplay)
        launchDescriptionBottomSheet(
            titleRes = R.string.swap_rate_title,
            descriptionText = description
        )
    } else {
        launchDescriptionBottomSheet(
            titleRes = R.string.swap_rate_title,
            descriptionRes = R.string.swap_rate_description
        )
    }
}

fun DescriptionBottomSheetLauncher.launchPriceDifferenceDescription() {
    launchDescriptionBottomSheet(
        titleRes = R.string.swap_price_difference_title,
        descriptionRes = R.string.swap_price_difference_description
    )
}

fun DescriptionBottomSheetLauncher.launchSlippageDescription() {
    launchDescriptionBottomSheet(
        titleRes = R.string.swap_slippage_title,
        descriptionRes = R.string.swap_slippage_description
    )
}
