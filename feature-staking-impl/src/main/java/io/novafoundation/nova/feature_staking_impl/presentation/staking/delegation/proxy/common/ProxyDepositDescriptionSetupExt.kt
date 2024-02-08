package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.common

import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_staking_impl.R

fun DescriptionBottomSheetLauncher.launchProxyDepositDescription() {
    launchDescriptionBottomSheet(
        titleRes = R.string.common_proxy_deposit,
        descriptionRes = R.string.add_proxy_deposit_description_message
    )
}
