package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.isOpen
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.KnownNovaPools
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.getPoolComparator
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.address
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.isActive
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.name
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.nameOrAddress
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.NominationPoolProvider
import io.novafoundation.nova.runtime.ext.addressOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchNominationPoolInteractor(
    private val nominationPoolProvider: NominationPoolProvider,
    private val knownNovaPools: KnownNovaPools
) {

    suspend fun getSortedNominationPools(stakingOption: StakingOption, coroutineScope: CoroutineScope): List<NominationPool> {
        val nominationPools = nominationPoolProvider.getNominationPools(stakingOption, coroutineScope)
        val comparator = getPoolComparator(knownNovaPools, stakingOption.chain)
        return nominationPools
            .filter { it.state.isOpen && it.status.isActive }
            .sortedWith(comparator)
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
                    (it.name()?.lowercase()?.contains(query) ?: false)
                        || it.address(stakingOption.chain).startsWith(query)
                }
                .sortedWith(comparator)
        }
    }
}
