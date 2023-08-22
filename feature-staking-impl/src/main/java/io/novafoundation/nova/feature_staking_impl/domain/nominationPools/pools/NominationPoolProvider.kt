package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.rewards.NominationPoolRewardCalculator
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import kotlinx.coroutines.CoroutineScope

interface NominationPoolProvider {

    suspend fun getNominationPools(
        stakingOption: StakingOption,
        computationScope: CoroutineScope,
    ): List<NominationPool>
}

class RealNominationPoolProvider(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val nominationPoolStateRepository: NominationPoolStateRepository,
    private val poolStateRepository: NominationPoolStateRepository
): NominationPoolProvider {

    override suspend fun getNominationPools(
        stakingOption: StakingOption,
        computationScope: CoroutineScope
    ): List<NominationPool> {
        val chainId = stakingOption.chain.id

        val rewardCalculator = nominationPoolSharedComputation.poolRewardCalculator(stakingOption, computationScope)
        val bondedPools = nominationPoolSharedComputation.allBondedPools(chainId, computationScope)

        val allPoolAccounts = nominationPoolSharedComputation.allBondedPoolAccounts(chainId, computationScope)
        val poolMetadatas = nominationPoolStateRepository.getPoolMetadatas(allPoolAccounts.keys, chainId)

        return bondedPools.map { (poolId, bondedPool) ->
            NominationPool(
                id = poolId,
                membersCount = bondedPool.memberCounter,
                status = rewardCalculator.getPoolStatus(poolId),
                metadata = poolMetadatas[poolId],
                state = bondedPool.state,
                icon = poolStateRepository.getPoolIcon(poolId, chainId),
                stashAccountId = allPoolAccounts.getValue(poolId).value,
            )
        }
    }

    private fun NominationPoolRewardCalculator.getPoolStatus(poolId: PoolId): NominationPool.Status {
        val apy = apyFor(poolId)

        return if (apy != null) {
            NominationPool.Status.Active(apy)
        } else {
            NominationPool.Status.Inactive
        }
    }
}
