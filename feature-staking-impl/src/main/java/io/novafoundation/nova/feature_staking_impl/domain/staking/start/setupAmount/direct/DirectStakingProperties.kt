package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.minStake
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.CoroutineScope

class DirectStakingPropertiesFactory(
    private val validatorRecommendatorFactory: ValidatorRecommendatorFactory,
    private val recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
    private val stakingSharedComputation: StakingSharedComputation,
): SingleStakingPropertiesFactory {

    override fun createProperties(scope: CoroutineScope, stakingOption: StakingOption): SingleStakingProperties {
        return DirectStakingProperties(
            validatorRecommendatorFactory = validatorRecommendatorFactory,
            recommendationSettingsProviderFactory = recommendationSettingsProviderFactory,
            stakingOption = stakingOption,
            scope = scope,
            stakingSharedComputation = stakingSharedComputation
        )
    }
}

private class DirectStakingProperties(
    validatorRecommendatorFactory: ValidatorRecommendatorFactory,
    recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
    private val stakingOption: StakingOption,
    private val scope: CoroutineScope,
    private val stakingSharedComputation: StakingSharedComputation,
): SingleStakingProperties {

    override fun availableBalance(asset: Asset): Balance {
        return asset.freeInPlanks
    }

    override val recommendation: SingleStakingRecommendation = DirectStakingRecommendation(
        validatorRecommendatorFactory = validatorRecommendatorFactory,
        recommendationSettingsProviderFactory = recommendationSettingsProviderFactory,
        stakingOption = stakingOption,
        scope = scope
    )


    override val validationSystem: StartMultiStakingValidationSystem = ValidationSystem {
        // TODO validations
    }

    override suspend fun minStake(): Balance {
        return stakingSharedComputation.minStake(stakingOption.chain.id, scope)
    }
}
