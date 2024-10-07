package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommenderFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class DirectStakingRecommendation(
    private val validatorRecommenderFactory: ValidatorRecommenderFactory,
    private val recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
    private val stakingConstantsRepository: StakingConstantsRepository,
    private val stakingOption: StakingOption,
    private val scope: CoroutineScope
) : SingleStakingRecommendation {

    private val recommendator = scope.async {
        validatorRecommenderFactory.create(scope)
    }

    private val recommendationSettingsProvider = scope.async {
        recommendationSettingsProviderFactory.create(scope)
    }

    override suspend fun recommendedSelection(stake: Balance): StartMultiStakingSelection {
        val provider = recommendationSettingsProvider.await()
        val maximumValidatorsPerNominator = stakingConstantsRepository.maxValidatorsPerNominator(stakingOption.chain.id, stake)
        val recommendationSettings = provider.recommendedSettings(maximumValidatorsPerNominator)
        val recommendator = recommendator.await()

        val recommendedValidators = recommendator.recommendations(recommendationSettings)

        return DirectStakingSelection(
            validators = recommendedValidators,
            validatorsLimit = maximumValidatorsPerNominator,
            stakingOption = stakingOption,
            stake = stake
        )
    }
}
