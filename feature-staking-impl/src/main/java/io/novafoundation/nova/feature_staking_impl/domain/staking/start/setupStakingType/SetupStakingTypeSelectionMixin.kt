package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.SelectionTypeSource
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.currentSelectionFlow
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct.DirectStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.NominationPoolSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

class SetupStakingTypeSelectionMixinFactory(
    private val currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
) {

    fun create(
        scope: CoroutineScope
    ): SetupStakingTypeSelectionMixin {
        return SetupStakingTypeSelectionMixin(
            currentSelectionStoreProvider,
            editableSelectionStoreProvider,
            scope
        )
    }
}

class SetupStakingTypeSelectionMixin(
    private val currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val scope: CoroutineScope
) {

    val editableSelectionFlow = editableSelectionStoreProvider.currentSelectionFlow(scope)
        .filterNotNull()

    val currentSelectionFlow = currentSelectionStoreProvider.currentSelectionFlow(scope)
        .filterNotNull()

    suspend fun apply() {
        val recommendableSelection = editableSelectionStoreProvider.getSelectionStore(scope).currentSelection ?: return
        currentSelectionStoreProvider.getSelectionStore(scope)
            .updateSelection(recommendableSelection)
    }

    suspend fun selectNominationPoolAndApply(pool: NominationPool) {
        val editingSelection = editableSelectionFlow.first().selection as? NominationPoolSelection ?: return
        val newSelection = editingSelection.copy(pool = pool)
        setSelectionAndApply(newSelection)
    }

    suspend fun selectValidatorsAndApply(validators: List<Validator>) {
        val editingSelection = editableSelectionFlow.first().selection as? DirectStakingSelection ?: return
        val newSelection = editingSelection.copy(validators = validators)
        setSelectionAndApply(newSelection)
    }

    private suspend fun setSelectionAndApply(selection: StartMultiStakingSelection) {
        currentSelectionStoreProvider.getSelectionStore(scope)
            .updateSelection(
                RecommendableMultiStakingSelection(
                    SelectionTypeSource.Manual(contentRecommended = false),
                    selection
                )
            )
    }
}
