package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.SelectionTypeSource
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStore
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

class AutomaticMultiStakingSelectionType(
    private val candidates: List<SingleStakingProperties>,
    private val selectionStore: StartMultiStakingSelectionStore,
) : MultiStakingSelectionType {

    override suspend fun validationSystem(): StartMultiStakingValidationSystem {
        TODO("use selected candidate validation system + apply balance gap validation")
    }

    override suspend fun availableBalance(asset: Asset): Balance {
        return candidates.maxOf { it.availableBalance(asset) }
    }

    override suspend fun updateSelectionFor(stake: Balance) {
        val selection = selectionFor(stake)
        val recommendableSelection = RecommendableMultiStakingSelection(
            source = SelectionTypeSource.Automatic,
            selection = selection
        )

        selectionStore.updateSelection(recommendableSelection)
    }

    private suspend fun selectionFor(stake: Balance): StartMultiStakingSelection {
        val candidate = candidates.firstAllowingToStake(stake) ?: candidates.findWithMinimumStake()

        return candidate.recommendation.recommendedSelection()
    }

    private suspend fun List<SingleStakingProperties>.firstAllowingToStake(stake: Balance): SingleStakingProperties? {
        return find { it.minStake() <= stake }
    }

    private suspend fun List<SingleStakingProperties>.findWithMinimumStake(): SingleStakingProperties {
        return minBy { it.minStake() }
    }
}
