package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.direct

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.asset
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.minStake
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.StakingTypesConflictValidation.ConflictingStakingType
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.StakingTypesConflictValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommenderFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.amountOf
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.positiveBond
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_staking_impl.domain.validations.maximumNominatorsReached
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.minimumBondValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class DirectStakingPropertiesFactory(
    private val validatorRecommenderFactory: ValidatorRecommenderFactory,
    private val recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
    private val stakingSharedComputation: StakingSharedComputation,
    private val stakingRepository: StakingRepository,
    private val stakingConstantsRepository: StakingConstantsRepository,
    private val stakingTypesConflictValidationFactory: StakingTypesConflictValidationFactory,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : SingleStakingPropertiesFactory {

    override fun createProperties(scope: CoroutineScope, stakingOption: StakingOption): SingleStakingProperties {
        return DirectStakingProperties(
            validatorRecommenderFactory = validatorRecommenderFactory,
            recommendationSettingsProviderFactory = recommendationSettingsProviderFactory,
            stakingOption = stakingOption,
            scope = scope,
            stakingSharedComputation = stakingSharedComputation,
            stakingRepository = stakingRepository,
            stakingConstantsRepository = stakingConstantsRepository,
            stakingTypesConflictValidationFactory = stakingTypesConflictValidationFactory,
            selectedAccountUseCase = selectedAccountUseCase
        )
    }
}

private class DirectStakingProperties(
    validatorRecommenderFactory: ValidatorRecommenderFactory,
    recommendationSettingsProviderFactory: RecommendationSettingsProviderFactory,
    private val stakingOption: StakingOption,
    private val scope: CoroutineScope,
    private val stakingSharedComputation: StakingSharedComputation,
    private val stakingRepository: StakingRepository,
    private val stakingConstantsRepository: StakingConstantsRepository,
    private val stakingTypesConflictValidationFactory: StakingTypesConflictValidationFactory,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : SingleStakingProperties {

    override val stakingType: Chain.Asset.StakingType = stakingOption.stakingType

    override suspend fun availableBalance(asset: Asset): Balance {
        return asset.freeInPlanks
    }

    override suspend fun maximumToStake(asset: Asset, fee: Balance): Balance {
        return availableBalance(asset) - fee
    }

    override val recommendation: SingleStakingRecommendation = DirectStakingRecommendation(
        stakingConstantsRepository = stakingConstantsRepository,
        validatorRecommenderFactory = validatorRecommenderFactory,
        recommendationSettingsProviderFactory = recommendationSettingsProviderFactory,
        stakingOption = stakingOption,
        scope = scope
    )

    override val validationSystem: StartMultiStakingValidationSystem = ValidationSystem {
        noConflictingStaking()

        maximumNominatorsReached()

        positiveBond()

        enoughForMinimumStake()

        enoughAvailableToStake()
    }

    override suspend fun minStake(): Balance {
        return stakingSharedComputation.minStake(stakingOption.chain.id, scope)
    }

    private fun StartMultiStakingValidationSystemBuilder.noConflictingStaking() {
        stakingTypesConflictValidationFactory.noStakingTypesConflict(
            accountId = { selectedAccountUseCase.getSelectedMetaAccount().requireAccountIdIn(it.recommendableSelection.selection.stakingOption.chain) },
            chainId = { it.asset.token.configuration.chainId },
            error = { StartMultiStakingValidationFailure.HasConflictingStakingType },
            checkStakingTypeNotPresent = ConflictingStakingType.POOLS
        )
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
            chainId = { stakingOption.chain.id },
            errorProducer = { StartMultiStakingValidationFailure.MaxNominatorsReached(stakingType) }
        )
    }

    private fun StartMultiStakingValidationSystemBuilder.enoughAvailableToStake() {
        sufficientBalance(
            fee = { it.fee },
            available = { it.amountOf(availableBalance(it.asset)) },
            amount = { it.amountOf(it.selection.stake) },
            error = { context ->
                StartMultiStakingValidationFailure.NotEnoughToPayFees(
                    chainAsset = context.payload.asset.token.configuration,
                    maxUsable = context.maxUsable,
                    fee = context.fee
                )
            }
        )
    }
}
