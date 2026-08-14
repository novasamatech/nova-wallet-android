package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.select.model

/**
 * Counters of the custom validators selection.
 *
 * Community counters describe the slots the user can actually operate on: [communityLimit] is what is left
 * after the locked (Nova) validators reserved their slots, [communitySelected] is how many of them are taken.
 * They drive "fill rest with recommended" and "deselect all".
 *
 * Total counters describe the whole nomination that will be submitted, locked validators included, and are
 * the ones shown to the user on the proceed button - what is displayed must match what is signed.
 */
class ValidatorSelectionState(
    val communitySelected: Int,
    val communityLimit: Int,
    val lockedSelected: Int,
    val totalLimit: Int,
) {

    val totalSelected: Int = communitySelected + lockedSelected

    fun isEmpty(): Boolean = totalSelected == 0
}
