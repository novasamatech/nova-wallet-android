package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.select.model

class ValidatorSelectionState(
    val communitySelected: Int,
    val communityLimit: Int,
    val lockedSelected: Int,
) {

    fun isEmpty(): Boolean = communitySelected == 0 && lockedSelected == 0
}
