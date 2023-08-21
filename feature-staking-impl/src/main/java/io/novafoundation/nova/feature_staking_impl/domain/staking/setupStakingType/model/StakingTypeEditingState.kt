package io.novafoundation.nova.feature_staking_impl.domain.staking.setupStakingType.model

class StakingTypeEditingState(
    val editableStakingTypes: List<EditableStakingType>,
    val dataHasChanged: Boolean
) {
}
