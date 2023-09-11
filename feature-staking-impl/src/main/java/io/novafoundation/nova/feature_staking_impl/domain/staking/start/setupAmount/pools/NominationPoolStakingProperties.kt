package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.pools

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolAvailableBalanceValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation.NominationPoolRecommenderFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.NominationPoolsAvailableBalanceResolver
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.stakeAmount
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationSystem
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
) : SingleStakingProperties {

    override val stakingType: Chain.Asset.StakingType = stakingOption.stakingType

    override suspend fun availableBalance(asset: Asset): Balance {
        return poolsAvailableBalanceResolver.availableBalanceToStartStaking(asset)
    }

    override suspend fun maximumToStake(asset: Asset, fee: Balance): Balance {
        return poolsAvailableBalanceResolver.maximumBalanceToStake(asset, fee).maxToStake
    }

    override val recommendation: SingleStakingRecommendation = NominationPoolRecommendation(
        scope = sharedComputationScope,
        stakingOption = stakingOption,
        nominationPoolRecommenderFactory = nominationPoolRecommenderFactory
    )

    override val validationSystem: StartMultiStakingValidationSystem = ValidationSystem {
        maxPoolMembersNotReached(nominationPoolGlobalsRepository)

        activePool()

        positiveBond()

        enoughForMinJoinBond()

        poolAvailableBalanceValidationFactory.enoughAvailableBalanceToStake(
            asset = { it.asset },
            fee = { it.fee.fee.amount },
            amount = { it.selection.stakeAmount() },
            error = StartMultiStakingValidationFailure::PoolAvailableBalance
        )
    }

    override suspend fun minStake(): Balance {
        return nominationPoolSharedComputation.minJoinBond(stakingOption.chain.id, sharedComputationScope)
    }
}
