package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store

import io.novafoundation.nova.common.utils.selectionStore.MutableSelectionStore
import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct.DirectStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.NominationPoolSelection
import java.math.BigInteger

class StartMultiStakingSelectionStore : MutableSelectionStore<RecommendableMultiStakingSelection>() {

    fun updateStake(amount: BigInteger) {
        val currentSelection = getCurrentSelection()
        currentSelection?.let {
            currentSelectionFlow.value = currentSelection.copy(
                selection = currentSelection.selection.copyWith(amount)
            )
        }
    }
}

fun StartMultiStakingSelectionStore.getValidatorsOrEmpty(): List<Validator> {
    val selection = getCurrentSelection()?.selection
    return if (selection is DirectStakingSelection) {
        selection.validators
    } else {
        emptyList()
    }
}

fun StartMultiStakingSelectionStore.getPoolOrNull(): NominationPool? {
    val selection = getCurrentSelection()?.selection
    return if (selection is NominationPoolSelection) {
        selection.pool
    } else {
        return null
    }
}
