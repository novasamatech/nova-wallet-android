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
    val description = when (mode) {
        is SwapRateDescriptionMode.Default -> resourceManager.getString(R.string.swap_rate_description)

        is SwapRateDescriptionMode.IncludesFee -> resourceManager.getString(R.string.swap_rate_includes_fee_description, mode.feePercentDisplay)
    }

    launchDescriptionBottomSheet(
        title = resourceManager.getString(R.string.swap_rate_title),
        description = description,
    )
}

fun DescriptionBottomSheetLauncher.launchPriceDifferenceDescription(resourceManager: ResourceManager) {
    launchDescriptionBottomSheet(
        title = resourceManager.getString(R.string.swap_price_difference_title),
        description = resourceManager.getString(R.string.swap_price_difference_description)
    )
}

fun DescriptionBottomSheetLauncher.launchSlippageDescription(resourceManager: ResourceManager) {
    launchDescriptionBottomSheet(
        title = resourceManager.getString(R.string.swap_slippage_title),
        description = resourceManager.getString(R.string.swap_slippage_description)
    )
}
