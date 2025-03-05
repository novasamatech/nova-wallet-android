package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.SelectionTypeSource
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.currentSelectionFlow
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct.DirectStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools.NominationPoolSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.math.BigInteger

class SetupStakingTypeSelectionMixinFactory(
    private val currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val singleStakingPropertiesFactory: SingleStakingPropertiesFactory
) {

    fun create(
        scope: CoroutineScope
    ): SetupStakingTypeSelectionMixin {
        return SetupStakingTypeSelectionMixin(
            currentSelectionStoreProvider,
            editableSelectionStoreProvider,
            singleStakingPropertiesFactory,
            scope
        )
    }
}

class SetupStakingTypeSelectionMixin(
    private val currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val editableSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val singleStakingPropertiesFactory: SingleStakingPropertiesFactory,
    private val scope: CoroutineScope
) {

    val editableSelectionFlow = editableSelectionStoreProvider.currentSelectionFlow(scope)
        .filterNotNull()

    suspend fun apply() {
        val recommendableSelection = editableSelectionStoreProvider.getSelectionStore(scope).getCurrentSelection() ?: return
        currentSelectionStoreProvider.getSelectionStore(scope)
            .updateSelection(recommendableSelection)
    }

    suspend fun selectNominationPoolAndApply(pool: NominationPool, stakingOption: StakingOption) {
        val editingSelection = editableSelectionFlow.first().selection as? NominationPoolSelection ?: return
        val newSelection = editingSelection.copy(pool = pool)
        setSelectionAndApply(newSelection, stakingOption)
    }

    suspend fun selectValidatorsAndApply(validators: List<Validator>, stakingOption: StakingOption) {
        val editingSelection = editableSelectionFlow.first().selection as? DirectStakingSelection ?: return
        val newSelection = editingSelection.copy(validators = validators)
        setSelectionAndApply(newSelection, stakingOption)
    }

    private suspend fun setSelectionAndApply(selection: StartMultiStakingSelection, stakingOption: StakingOption) {
        val properties = singleStakingPropertiesFactory.createProperties(scope, stakingOption)
        val recommendedSelection = properties.recommendation.recommendedSelection(selection.stake)

        // Content is not recommended if recommendation does not exist
        val contentRecommended = recommendedSelection?.isSettingsEquals(selection) ?: false

        currentSelectionStoreProvider.getSelectionStore(scope)
            .updateSelection(
                RecommendableMultiStakingSelection(
                    SelectionTypeSource.Manual(contentRecommended = contentRecommended),
                    selection,
                    properties
                )
            )
    }

    suspend fun selectRecommended(viewModelScope: CoroutineScope, stakingOption: StakingOption, amount: BigInteger) {
        val properties = singleStakingPropertiesFactory.createProperties(scope, stakingOption)
        val recommendedSelection = singleStakingPropertiesFactory.createProperties(scope, stakingOption)
            .recommendation
            .recommendedSelection(amount) ?: return

        val recommendableMultiStakingSelection = RecommendableMultiStakingSelection(
            source = SelectionTypeSource.Manual(contentRecommended = true),
            selection = recommendedSelection,
            properties = properties
        )

        editableSelectionStoreProvider.getSelectionStore(viewModelScope)
            .updateSelection(recommendableMultiStakingSelection)
    }
}
