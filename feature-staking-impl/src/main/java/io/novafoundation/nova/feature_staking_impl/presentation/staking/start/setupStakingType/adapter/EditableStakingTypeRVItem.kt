package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType.adapter

import androidx.annotation.DrawableRes
import io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget.StakingTargetModel

class EditableStakingTypeRVItem(
    val isSelected: Boolean,
    val isSelectable: Boolean,
    val title: String,
    @DrawableRes val imageRes: Int,
    val conditions: List<String>,
    val stakingTarget: StakingTarget?
) {

    interface StakingTarget {

        object Loading : StakingTarget

        data class Model(val model: StakingTargetModel) : StakingTarget
    }
}
