package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.unbondings

import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.UnlockChunk
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.UnbondingPool
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.UnbondingPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolUnbondRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.constructUnbondingList
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.emitAll
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
    private val nominationPoolUnbondRepository: NominationPoolUnbondRepository,
    private val stakingSharedComputation: StakingSharedComputation,
) : NominationPoolUnbondingsInteractor {

    override fun unbondingsFlow(
        poolMember: PoolMember,
        stakingOption: StakingOption,
        sharedComputationScope: CoroutineScope,
    ): Flow<Unbondings> {
        val chainId = stakingOption.assetWithChain.chain.id
        return combineTransform(
            stakingSharedComputation.activeEraFlow(chainId, sharedComputationScope),
            nominationPoolUnbondRepository.unbondingPoolsFlow(poolMember.poolId, chainId),
        ) { activeEraIndex, unbondingPools ->
            val unbondingsFlow = unbondingPools.unbondingsFor(poolMember, activeEraIndex, stakingOption, sharedComputationScope)
                .map { Unbondings.from(it, rebondPossible = false) }

            emitAll(unbondingsFlow)
        }
    }

    private fun UnbondingPools?.unbondingsFor(
        poolMember: PoolMember,
        activeEra: EraIndex,
        stakingOption: StakingOption,
        sharedComputationScope: CoroutineScope,
    ): Flow<List<Unbonding>> {
        if (this == null) return flowOf(emptyList())

        val unlockChunks = poolMember.unbondingEras.map { (unbondEra, unbondPoints) ->
            val unbondingPool = getPool(unbondEra)
            val unbondBalance = unbondingPool.amountOf(unbondPoints)

            UnlockChunk(amount = unbondBalance, era = unbondEra)
        }

        return stakingSharedComputation.constructUnbondingList(
            eraRedeemables = unlockChunks,
            activeEra = activeEra,
            stakingOption = stakingOption,
            sharedComputationScope = sharedComputationScope
        )
    }

    private fun UnbondingPools.getPool(era: EraIndex): UnbondingPool {
        return withEra[era] ?: noEra
    }
}
