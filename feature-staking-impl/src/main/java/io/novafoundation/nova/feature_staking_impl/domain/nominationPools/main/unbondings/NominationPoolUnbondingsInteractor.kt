package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.unbondings

import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.UnbondingPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.unlockChunksFor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.constructUnbondingList
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface NominationPoolUnbondingsInteractor {

    fun unbondingsFlow(
        poolMember: PoolMember,
        stakingOption: StakingOption,
        sharedComputationScope: CoroutineScope,
    ): Flow<Unbondings>
}

class RealNominationPoolUnbondingsInteractor(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val stakingSharedComputation: StakingSharedComputation,
) : NominationPoolUnbondingsInteractor {

    override fun unbondingsFlow(
        poolMember: PoolMember,
        stakingOption: StakingOption,
        sharedComputationScope: CoroutineScope,
    ): Flow<Unbondings> {
        val chainId = stakingOption.assetWithChain.chain.id
        return combineToPair(
            stakingSharedComputation.activeEraFlow(chainId, sharedComputationScope),
            nominationPoolSharedComputation.unbondingPoolsFlow(poolMember.poolId, chainId, sharedComputationScope),
        )
            .flatMapLatest { (activeEraIndex, unbondingPools) ->
                unbondingPools.unbondingsFor(poolMember, activeEraIndex, stakingOption, sharedComputationScope)
            }
            .map { Unbondings.from(it, rebondPossible = false) }
    }

    private fun UnbondingPools?.unbondingsFor(
        poolMember: PoolMember,
        activeEra: EraIndex,
        stakingOption: StakingOption,
        sharedComputationScope: CoroutineScope,
    ): Flow<List<Unbonding>> {
        if (this == null) return flowOf(emptyList())

        val unlockChunks = unlockChunksFor(poolMember)

        return stakingSharedComputation.constructUnbondingList(
            eraRedeemables = unlockChunks,
            activeEra = activeEra,
            stakingOption = stakingOption,
            sharedComputationScope = sharedComputationScope
        )
    }
}
