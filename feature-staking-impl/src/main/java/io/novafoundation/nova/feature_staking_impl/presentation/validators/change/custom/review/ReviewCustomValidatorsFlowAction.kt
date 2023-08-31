package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review

import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.SelectionTypeSource
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct.DirectStakingSelection
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.getSelectedValidators
import kotlinx.coroutines.CoroutineScope

interface ReviewValidatorsFlowAction {

    suspend fun execute(coroutineScope: CoroutineScope)
}

class EmptyReviewValidatorsFlowAction : ReviewValidatorsFlowAction {

    override suspend fun execute(coroutineScope: CoroutineScope) {
        // Do nothing
    }
}

class SetupStakingReviewValidatorsFlowAction(
    private val sharedStateSetup: SetupStakingSharedState,
    private val currentSelectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
) : ReviewValidatorsFlowAction {

    override suspend fun execute(coroutineScope: CoroutineScope) {
        val recommendationSettingsProvider = recommendationSettingsProviderFactory.create(coroutineScope)
        val selectionStore = currentSelectionStoreProvider.getSelectionStore(coroutineScope)

        val selectedValidators = sharedStateSetup.getSelectedValidators()
        val currentSelection = selectionStore.currentSelection?.selection ?: return
        val validatorsLimit = recommendationSettingsProvider.maximumValidatorsPerNominator

        val newSelection = RecommendableMultiStakingSelection(
            source = SelectionTypeSource.Manual(contentRecommended = false),
            selection = DirectStakingSelection(
                validators = selectedValidators,
                validatorsLimit = validatorsLimit,
                stakingOption = currentSelection.stakingOption,
                stake = currentSelection.stake
            )
        )

        selectionStore.updateSelection(newSelection)
    }
}
