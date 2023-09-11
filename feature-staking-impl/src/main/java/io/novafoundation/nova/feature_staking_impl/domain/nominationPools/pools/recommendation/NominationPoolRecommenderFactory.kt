package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.isOpen
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.KnownNovaPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.getPoolComparator
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.isActive
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.NominationPoolProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class NominationPoolRecommenderFactory(
    private val computationalCache: ComputationalCache,
    private val nominationPoolProvider: NominationPoolProvider,
    private val knownNovaPools: KnownNovaPools,
    private val nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
) {

    suspend fun create(stakingOption: StakingOption, computationScope: CoroutineScope): NominationPoolRecommender {
        val key = "NominationPoolRecommender"

        return computationalCache.useCache(key, computationScope) {
            val nominationPools = nominationPoolProvider.getNominationPools(stakingOption, computationScope)
            val maxPoolMembersPerPool = nominationPoolGlobalsRepository.maxPoolMembersPerPool(stakingOption.chain.id)

            RealNominationPoolRecommender(
                chain = stakingOption.chain,
                allNominationPools = nominationPools,
                maxPoolMembersPerPool = maxPoolMembersPerPool,
                poolComparator = getPoolComparator(knownNovaPools, stakingOption.chain)
            )
        }
    }
}

private class RealNominationPoolRecommender(
    private val chain: Chain,
    private val allNominationPools: List<NominationPool>,
    private val maxPoolMembersPerPool: Int?,
    private val poolComparator: Comparator<NominationPool>
) : NominationPoolRecommender {

    override val recommendations = constructRecommendationList()

    override fun recommendedPool(): NominationPool {
        return recommendations.first()
    }

    private fun constructRecommendationList(): List<NominationPool> {
        return allNominationPools
            .filter { it.status.isActive && it.canBeJoined() }
            // weaken filter conditions if no matching pools were found
            .ifEmpty { allNominationPools.filter { it.canBeJoined() } }
            .sortedWith(poolComparator)
    }

    private fun NominationPool.canBeJoined(): Boolean {
        return state.isOpen && hasFreeSpots()
    }

    private fun NominationPool.hasFreeSpots(): Boolean {
        return maxPoolMembersPerPool == null || membersCount < maxPoolMembersPerPool
    }
}
