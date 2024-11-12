package io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description

import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_swap_api.R

fun DescriptionBottomSheetLauncher.launchSwapRateDescription() {
    launchDescriptionBottomSheet(
        titleRes = R.string.swap_rate_title,
        descriptionRes = R.string.swap_rate_description
    )
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
