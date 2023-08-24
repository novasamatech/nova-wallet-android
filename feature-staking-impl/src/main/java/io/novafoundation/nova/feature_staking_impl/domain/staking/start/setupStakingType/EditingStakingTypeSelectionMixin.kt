package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.SelectionTypeSource
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.currentSelectionFlow
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

class EditingStakingTypeSelectionMixinFactory(
    private val currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val stakingTypeDetailsProviders: List<StakingTypeDetailsProvider>,
) {

    fun create(scope: CoroutineScope): EditingStakingTypeSelectionMixin {
        return EditingStakingTypeSelectionMixin(
            currentSelectionStoreProvider,
            editableSelectionStoreProvider,
            stakingTypeDetailsProviders,
            scope
        )
    }

}

class EditingStakingTypeSelectionMixin(
    private val currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val stakingTypeDetailsProviders: List<StakingTypeDetailsProvider>,
    private val scope: CoroutineScope
) {

    val editableSelectionFlow = editableSelectionStoreProvider.currentSelectionFlow(scope)
        .filterNotNull()

    val currentSelectionFlow = currentSelectionStoreProvider.currentSelectionFlow(scope)
        .filterNotNull()

    val availableToRewriteData = combine(
        currentSelectionFlow,
        editableSelectionFlow
    ) { current, editable ->
        current != editable
    }

    suspend fun setRecommendedSelection(stakingType: Chain.Asset.StakingType) {
        val recommendedSelection = stakingTypeDetailsProviders.firstOrNull { it.stakingType == stakingType }
            ?.recommendationProvider
            ?.recommendedSelection() ?: return

        val recommendableMultiStakingSelection = RecommendableMultiStakingSelection(
            source = SelectionTypeSource.Manual(contentRecommended = true),
            selection = recommendedSelection
        )

        editableSelectionStoreProvider.getSelectionStore(scope)
            .updateSelection(recommendableMultiStakingSelection)
    }

    suspend fun apply() {
        editableSelectionStoreProvider.getSelectionStore(scope).currentSelection?.let {
            currentSelectionStoreProvider.getSelectionStore(scope).updateSelection(it)
        }
    }
}
