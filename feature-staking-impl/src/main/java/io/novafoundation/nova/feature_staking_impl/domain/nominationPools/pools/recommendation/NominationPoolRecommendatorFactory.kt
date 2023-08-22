package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.isOpen
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.KnownNovaPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.isNovaPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.apy
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.isActive
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.NominationPoolProvider
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope

class NominationPoolRecommendatorFactory(
    private val computationalCache: ComputationalCache,
    private val nominationPoolProvider: NominationPoolProvider,
    private val knownNovaPools: KnownNovaPools,
) {

    suspend fun create(stakingOption: StakingOption, computationScope: CoroutineScope): NominationPoolRecommendator {
        val key = "NominationPoolRecommendator"

        return computationalCache.useCache(key, computationScope) {
            val nominationPools = nominationPoolProvider.getNominationPools(stakingOption, computationScope)

            RealNominationPoolRecommendator(
                chain = stakingOption.chain,
                allNominationPools = nominationPools,
                knownNovaPools = knownNovaPools
            )
        }
    }
}


private class RealNominationPoolRecommendator(
    private val chain: Chain,
    private val allNominationPools: List<NominationPool>,
    private val knownNovaPools: KnownNovaPools,
) : NominationPoolRecommendator {

    private val recommendations = constructRecommendationList()

    override fun recommendedPool(): NominationPool {
        return recommendations.first()
    }

    private fun constructRecommendationList(): List<NominationPool> {
        return allNominationPools
            .filter { it.status.isActive && it.state.isOpen }
            // weaken filter conditions if no matching pools found
            .ifEmpty { allNominationPools.filter { it.state.isOpen } }
            .sortedWith(poolComparator())
    }

    private fun poolComparator(): Comparator<NominationPool> {
        return compareByDescending<NominationPool> { pool -> knownNovaPools.isNovaPool(chain.id, pool.id) }
            .thenByDescending { it.apy.orZero() }
            .thenByDescending { it.membersCount }
    }
}
