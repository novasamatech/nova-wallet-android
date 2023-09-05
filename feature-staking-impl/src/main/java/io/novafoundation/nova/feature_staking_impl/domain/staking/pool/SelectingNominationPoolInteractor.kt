package io.novafoundation.nova.feature_staking_impl.domain.staking.pool

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.apy
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.NominationPoolProvider
import kotlinx.coroutines.CoroutineScope

class SelectingNominationPoolInteractor(
    private val nominationPoolProvider: NominationPoolProvider
) {

    suspend fun getSortedNominationPools(stakingOption: StakingOption, coroutineScope: CoroutineScope): List<NominationPool> {
        val nominationPools = nominationPoolProvider.getNominationPools(stakingOption, coroutineScope)
        val comparator = compareByDescending<NominationPool> { it.apy }
            .thenByDescending { it.membersCount }
        return nominationPools.sortedWith(comparator)
    }
}
