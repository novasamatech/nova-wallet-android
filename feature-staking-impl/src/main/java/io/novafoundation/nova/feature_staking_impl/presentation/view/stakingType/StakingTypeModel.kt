package io.novafoundation.nova.feature_staking_impl.presentation.view.stakingType

import io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget.StakingTargetModel

class StakingTypeModel(
    val title: String,
    val isSelectable: Boolean,
    val conditions: List<String>,
    val stakingTarget: StakingTarget?,
) {
    interface StakingTarget {

        object Loading : StakingTarget

        class Model(val model: StakingTargetModel) : StakingTarget
    }
}
