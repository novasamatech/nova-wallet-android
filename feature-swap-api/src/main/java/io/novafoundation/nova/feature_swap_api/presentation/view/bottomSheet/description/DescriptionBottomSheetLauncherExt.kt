package io.novafoundation.nova.feature_swap_api.presentation.view.bottomSheet.description

import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_swap_api.R

fun DescriptionBottomSheetLauncher.launchSwapRateDescription() {
    launchDescriptionBottomSheet(
        titleRes = R.string.swap_rate_title,
        descriptionRes = R.string.swap_rate_description
    )
}
