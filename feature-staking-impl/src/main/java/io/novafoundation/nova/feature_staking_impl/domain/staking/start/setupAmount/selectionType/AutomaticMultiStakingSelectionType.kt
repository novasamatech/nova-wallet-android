package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.copyIntoCurrent
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.RecommendableMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.SelectionTypeSource
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStore
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.availableBalanceGapValidation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull

class AutomaticMultiStakingSelectionType(
    private val candidates: List<SingleStakingProperties>,
    private val selectionStore: StartMultiStakingSelectionStore,
    private val locksRepository: BalanceLocksRepository,
) : MultiStakingSelectionType {

    override suspend fun validationSystem(selection: StartMultiStakingSelection): StartMultiStakingValidationSystem {
        val candidateValidationSystem = candidates.first { it.stakingType == selection.stakingOption.stakingType }
            .validationSystem

        return ValidationSystem {
            // should always go before `candidateValidationSystem` since it delegates some cases to type-specific validations
            availableBalanceGapValidation(
                candidates = candidates,
                locksRepository = locksRepository
            )

            candidateValidationSystem.copyIntoCurrent()
        }
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
        val candidatesWithMinStakes = candidates.map { it to it.minStake() }

        val result = candidatesWithMinStakes.sortedWith(
            // Prioritize sources that fit user min stake
            compareBy<Pair<SingleStakingProperties, Balance>> { (_, minStake) -> minStake <= stake }
                // If user stake is so low he cant stake with neither source, take the most affordable ones
                .thenBy { (_, minStake) -> minStake }
        ).tryFindNonNull { (candidate, _) ->
            candidate.recommendation.recommendedSelection(stake)
        }

        // TODO in the extreme situation no source can provide recommendation, crash
        // We can handle it better but it would require deeper changes in presentation layer
        return requireNotNull(result) {
            "No recommendations is available"
        }
    }
}
