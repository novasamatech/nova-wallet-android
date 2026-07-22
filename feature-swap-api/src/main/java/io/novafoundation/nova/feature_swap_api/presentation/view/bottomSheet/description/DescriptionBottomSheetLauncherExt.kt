package io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_swap_api.R

sealed class SwapRateDescriptionMode {

    object Default : SwapRateDescriptionMode()

    data class IncludesFee(val feePercentDisplay: String) : SwapRateDescriptionMode()
}

fun DescriptionBottomSheetLauncher.launchSwapRateDescription(
    resourceManager: ResourceManager,
    mode: SwapRateDescriptionMode,
) {
    when (mode) {
        is SwapRateDescriptionMode.Default -> launchDescriptionBottomSheet(
            titleRes = R.string.swap_rate_title,
            descriptionRes = R.string.swap_rate_description,
        )

        is SwapRateDescriptionMode.IncludesFee -> launchDescriptionBottomSheet(
            titleRes = R.string.swap_rate_title,
            descriptionText = resourceManager.getString(R.string.swap_rate_includes_fee_description, mode.feePercentDisplay),
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
