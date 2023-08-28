package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class DirectStakingRecommendation(
    private val validatorRecommendatorFactory: ValidatorRecommendatorFactory,
    private val recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
    private val stakingOption: StakingOption,
    private val scope: CoroutineScope
) : SingleStakingRecommendation {

    private val recommendator = scope.async {
        validatorRecommendatorFactory.create(scope)
    }

    private val recommendationSettingsProvider = scope.async {
        recommendationSettingsProviderFactory.create(scope)
    }

    override suspend fun recommendedSelection(stake: Balance): StartMultiStakingSelection {
        val provider = recommendationSettingsProvider.await()
        val recommendationSettings = provider.defaultSettings()
        val recommendator = recommendator.await()

        val recommendedValidators = recommendator.recommendations(recommendationSettings)

        return DirectStakingSelection(
            validators = recommendedValidators,
            validatorsLimit = provider.maximumValidatorsPerNominator,
            stakingOption = stakingOption,
            stake = stake
        )
    }
}
