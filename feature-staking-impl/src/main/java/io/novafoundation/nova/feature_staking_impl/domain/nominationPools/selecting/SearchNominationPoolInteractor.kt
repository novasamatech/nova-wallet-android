package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting

import io.novafoundation.nova.common.utils.orFalse
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.KnownNovaPools
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.getPoolComparator
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.address
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.name
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.nameOrAddress
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.NominationPoolProvider
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation.NominationPoolRecommenderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchNominationPoolInteractor(
    private val nominationPoolProvider: NominationPoolProvider,
    private val knownNovaPools: KnownNovaPools,
    private val nominationPoolRecommenderFactory: NominationPoolRecommenderFactory
) {

    suspend fun getSortedNominationPools(stakingOption: StakingOption, coroutineScope: CoroutineScope): List<NominationPool> {
        return nominationPoolRecommenderFactory.create(stakingOption, coroutineScope)
            .recommendations
    }

    suspend fun searchNominationPools(
        queryFlow: Flow<String>,
        stakingOption: StakingOption,
        coroutineScope: CoroutineScope
    ): Flow<List<NominationPool>> {
        val nominationPools = nominationPoolProvider.getNominationPools(stakingOption, coroutineScope)
        val comparator = getPoolComparator(knownNovaPools, stakingOption.chain)
            .thenComparing { pool: NominationPool -> pool.nameOrAddress(stakingOption.chain) }
        return queryFlow.map { query ->
            if (query.isEmpty()) {
                return@map emptyList()
            }

            nominationPools
                .filter {
                    val name = it.name()?.lowercase()
                    val address = it.address(stakingOption.chain)
                    name?.contains(query).orFalse() || address.startsWith(query)
                }
                .sortedWith(comparator)
        }
    }
}
