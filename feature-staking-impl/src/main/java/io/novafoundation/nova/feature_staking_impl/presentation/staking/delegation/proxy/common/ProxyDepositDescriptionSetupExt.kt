package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_staking_impl.R

fun DescriptionBottomSheetLauncher.launchProxyDepositDescription(resourceManager: ResourceManager) {
    launchDescriptionBottomSheet(
        title = resourceManager.getString(R.string.common_proxy_deposit),
        description = resourceManager.getString(R.string.add_proxy_deposit_description_message)
    )
}
