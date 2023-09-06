package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.KnownNovaPools
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.getPoolComparator
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.NominationPoolProvider
import kotlinx.coroutines.CoroutineScope

class SelectingNominationPoolInteractor(
    private val nominationPoolProvider: NominationPoolProvider,
    private val knownNovaPools: KnownNovaPools
) {

    suspend fun getSortedNominationPools(stakingOption: StakingOption, coroutineScope: CoroutineScope): List<NominationPool> {
        val nominationPools = nominationPoolProvider.getNominationPools(stakingOption, coroutineScope)
        val comparator = getPoolComparator(knownNovaPools, stakingOption.chain)
        return nominationPools.sortedWith(comparator)
    }
}
