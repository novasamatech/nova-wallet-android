package io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget

import androidx.annotation.ColorRes
import io.novafoundation.nova.common.utils.images.Icon as CommonIcon

class StakingTargetModel(
    val title: String,
    val subtitle: String?,
    @ColorRes val subtitleColorRes: Int,
    val icon: TargetIcon?
) {

    sealed interface TargetIcon {

        class Quantity(val quantity: String) : TargetIcon

        class Icon(val icon: CommonIcon) : TargetIcon
    }
}
