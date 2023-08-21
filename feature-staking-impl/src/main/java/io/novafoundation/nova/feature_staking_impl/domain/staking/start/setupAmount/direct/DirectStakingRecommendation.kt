package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.StartMultiStakingSelection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class DirectStakingRecommendation(
    private val validatorRecommendatorFactory: ValidatorRecommendatorFactory,
    private val recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
    private val stakingOption: StakingOption,
    private val scope: CoroutineScope,
): SingleStakingRecommendation {

    private val recommendator = scope.async {
        validatorRecommendatorFactory.create(scope)
    }

    private val recommendationSettingsProvider = scope.async {
        recommendationSettingsProviderFactory.create(scope)
    }

    override suspend fun recommendedSelection(): StartMultiStakingSelection {
        val provider = recommendationSettingsProvider.await()
        val recommendationSettings = provider.defaultSettings()
        val recommendator = recommendator.await()

        val recommendedValidators = recommendator.recommendations(recommendationSettings)

        return DirectStakingSelection(
            validators = recommendedValidators,
            validatorsLimit = provider.maximumValidatorsPerNominator,
            stakingOption = stakingOption
        )
    }
}
