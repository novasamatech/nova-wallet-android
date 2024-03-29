package io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget

import io.novafoundation.nova.common.presentation.ColoredText
import io.novafoundation.nova.common.utils.images.Icon as CommonIcon

data class StakingTargetModel(
    val title: String,
    val subtitle: ColoredText?,
    val icon: TargetIcon?
) {

    sealed interface TargetIcon {

        data class Quantity(val quantity: String) : TargetIcon

        class Icon(val icon: CommonIcon) : TargetIcon
    }
}
