package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.model

import io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget.StakingTargetModel

sealed class StakingPropertiesModel {

    object Hidden : StakingPropertiesModel()

    object Loading : StakingPropertiesModel()

    class Loaded(val content: Content) : StakingPropertiesModel()

    class Content(
        val estimatedReward: String,
        val selection: StakingTargetModel
    )
}
