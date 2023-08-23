package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.asset
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.minStake
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.enoughToPayFee
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.positiveBond
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_staking_impl.domain.validations.maximumNominatorsReached
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.minimumBondValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class DirectStakingPropertiesFactory(
    private val validatorRecommendatorFactory: ValidatorRecommendatorFactory,
    private val recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
    private val stakingSharedComputation: StakingSharedComputation,
    private val stakingRepository: StakingRepository,
) : SingleStakingPropertiesFactory {

    override fun createProperties(scope: CoroutineScope, stakingOption: StakingOption): SingleStakingProperties {
        return DirectStakingProperties(
            validatorRecommendatorFactory = validatorRecommendatorFactory,
            recommendationSettingsProviderFactory = recommendationSettingsProviderFactory,
            stakingOption = stakingOption,
            scope = scope,
            stakingSharedComputation = stakingSharedComputation,
            stakingRepository = stakingRepository
        )
    }
}

private class DirectStakingProperties(
    validatorRecommendatorFactory: ValidatorRecommendatorFactory,
    recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
    private val stakingOption: StakingOption,
    private val scope: CoroutineScope,
    private val stakingSharedComputation: StakingSharedComputation,
    private val stakingRepository: StakingRepository,
) : SingleStakingProperties {

    override val stakingType: Chain.Asset.StakingType = stakingOption.stakingType

    override suspend fun availableBalance(asset: Asset): Balance {
        return asset.freeInPlanks
    }

    override val recommendation: SingleStakingRecommendation = DirectStakingRecommendation(
        validatorRecommendatorFactory = validatorRecommendatorFactory,
        recommendationSettingsProviderFactory = recommendationSettingsProviderFactory,
        stakingOption = stakingOption,
        scope = scope
    )

    override val validationSystem: StartMultiStakingValidationSystem = ValidationSystem {
        maximumNominatorsReached()

        enoughToPayFee()

        enoughForMinimumStake()

        positiveBond()
    }

    override suspend fun minStake(): Balance {
        return stakingSharedComputation.minStake(stakingOption.chain.id, scope)
    }

    private fun StartMultiStakingValidationSystemBuilder.enoughForMinimumStake() {
        minimumBondValidation(
            stakingRepository = stakingRepository,
            stakingSharedComputation = stakingSharedComputation,
            chainAsset = { stakingOption.asset },
            balanceToCheckAgainstRequired = { it.selection.stake },
            balanceToCheckAgainstRecommended = { it.selection.stake },
            error = StartMultiStakingValidationFailure::AmountLessThanMinimum
        )
    }

    private fun StartMultiStakingValidationSystemBuilder.maximumNominatorsReached() {
        maximumNominatorsReached(
            stakingRepository = stakingRepository,
            isAlreadyNominating = { false },
            chainId = { stakingOption.chain.id },
            errorProducer = { StartMultiStakingValidationFailure.MaxNominatorsReached(stakingType) }
        )
    }
}
