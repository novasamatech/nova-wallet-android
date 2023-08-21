package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface StartMultiStakingSelectionStore {

    val currentSelectionFlow: Flow<RecommendableMultiStakingSelection?>

    val currentSelection: RecommendableMultiStakingSelection?

    fun updateSelection(multiStakingSelection: RecommendableMultiStakingSelection)
}

class RealStartMultiStakingSelectionStore : StartMultiStakingSelectionStore {

    override val currentSelectionFlow = MutableStateFlow<RecommendableMultiStakingSelection?>(null)

    override val currentSelection: RecommendableMultiStakingSelection?
        get() = currentSelectionFlow.value

    override fun updateSelection(multiStakingSelection: RecommendableMultiStakingSelection) {
        currentSelectionFlow.value = multiStakingSelection
    }
}
