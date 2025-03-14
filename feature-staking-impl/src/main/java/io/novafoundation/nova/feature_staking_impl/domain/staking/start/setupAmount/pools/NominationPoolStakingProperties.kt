package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolAvailableBalanceValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.StakingTypesConflictValidation.ConflictingStakingType
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.StakingTypesConflictValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation.NominationPoolRecommenderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.NominationPoolsAvailableBalanceResolver
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.stakeAmount
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystemBuilder
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.nominationPools.activePool
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.nominationPools.enoughForMinJoinBond
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.nominationPools.maxPoolMembersNotReached
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.positiveBond
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingProperties
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingPropertiesFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.SingleStakingRecommendation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class NominationPoolStakingPropertiesFactory(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val nominationPoolRecommenderFactory: NominationPoolRecommenderFactory,
    private val poolsAvailableBalanceResolver: NominationPoolsAvailableBalanceResolver,
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
    private val poolAvailableBalanceValidationFactory: PoolAvailableBalanceValidationFactory,
    private val stakingTypesConflictValidationFactory: StakingTypesConflictValidationFactory,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : SingleStakingPropertiesFactory {

    override fun createProperties(scope: CoroutineScope, stakingOption: StakingOption): SingleStakingProperties {
        return NominationPoolStakingProperties(
            nominationPoolSharedComputation = nominationPoolSharedComputation,
            nominationPoolRecommenderFactory = nominationPoolRecommenderFactory,
            sharedComputationScope = scope,
            stakingOption = stakingOption,
            poolsAvailableBalanceResolver = poolsAvailableBalanceResolver,
            nominationPoolGlobalsRepository = nominationPoolGlobalsRepository,
            poolAvailableBalanceValidationFactory = poolAvailableBalanceValidationFactory,
            stakingTypesConflictValidationFactory = stakingTypesConflictValidationFactory,
            selectedAccountUseCase = selectedAccountUseCase
        )
    }
}

private class NominationPoolStakingProperties(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val nominationPoolRecommenderFactory: NominationPoolRecommenderFactory,
    private val sharedComputationScope: CoroutineScope,
    private val stakingOption: StakingOption,
    private val poolsAvailableBalanceResolver: NominationPoolsAvailableBalanceResolver,
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
    private val poolAvailableBalanceValidationFactory: PoolAvailableBalanceValidationFactory,
    private val stakingTypesConflictValidationFactory: StakingTypesConflictValidationFactory,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : SingleStakingProperties {

    override val stakingType: Chain.Asset.StakingType = stakingOption.stakingType

    override suspend fun availableBalance(asset: Asset): Balance {
        return poolsAvailableBalanceResolver.availableBalanceToStartStaking(asset)
    }

    override suspend fun maximumToStake(asset: Asset): Balance {
        return poolsAvailableBalanceResolver.maximumBalanceToStake(asset)
    }

    override val recommendation: SingleStakingRecommendation = NominationPoolRecommendation(
        scope = sharedComputationScope,
        stakingOption = stakingOption,
        nominationPoolRecommenderFactory = nominationPoolRecommenderFactory
    )

    override val validationSystem: StartMultiStakingValidationSystem = ValidationSystem {
        noConflictingStaking()

        maxPoolMembersNotReached(nominationPoolGlobalsRepository)

        activePool()

        positiveBond()

        enoughForMinJoinBond()

        poolAvailableBalanceValidationFactory.enoughAvailableBalanceToStake(
            asset = { it.asset },
            fee = { it.fee },
            amount = { it.selection.stakeAmount() },
            error = StartMultiStakingValidationFailure::PoolAvailableBalance
        )
    }

    private fun StartMultiStakingValidationSystemBuilder.noConflictingStaking() {
        stakingTypesConflictValidationFactory.noStakingTypesConflict(
            accountId = { selectedAccountUseCase.getSelectedMetaAccount().requireAccountIdIn(it.recommendableSelection.selection.stakingOption.chain) },
            chainId = { it.asset.token.configuration.chainId },
            error = { StartMultiStakingValidationFailure.HasConflictingStakingType },
            checkStakingTypeNotPresent = ConflictingStakingType.DIRECT
        )
    }

    override suspend fun minStake(): Balance {
        return nominationPoolSharedComputation.minJoinBond(stakingOption.chain.id, sharedComputationScope)
    }
}
