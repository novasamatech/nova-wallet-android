package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface StartMultiStakingSelectionStore {

    val currentSelectionFlow: Flow<RecommendableMultiStakingSelection?>

    val currentSelection: RecommendableMultiStakingSelection?

    fun updateSelection(multiStakingSelection: RecommendableMultiStakingSelection)

    fun updateStake(amount: BigInteger)
}

class RealStartMultiStakingSelectionStore : StartMultiStakingSelectionStore {

    override val currentSelectionFlow = MutableStateFlow<RecommendableMultiStakingSelection?>(null)

    override val currentSelection: RecommendableMultiStakingSelection?
        get() = currentSelectionFlow.value

    override fun updateSelection(multiStakingSelection: RecommendableMultiStakingSelection) {
        currentSelectionFlow.value = multiStakingSelection
    }

    override fun updateStake(amount: BigInteger) {
        currentSelection?.let { currentSelection ->
            currentSelectionFlow.value = currentSelection.copy(
                selection = currentSelection.selection.copyWith(amount)
            )
        }
    }
}
