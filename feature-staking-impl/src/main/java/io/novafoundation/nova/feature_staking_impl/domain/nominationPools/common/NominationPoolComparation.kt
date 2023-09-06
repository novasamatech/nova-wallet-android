package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.KnownNovaPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.isNovaPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.apy
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun getPoolComparator(knownNovaPools: KnownNovaPools, chain: Chain): Comparator<NominationPool> {
    return compareByDescending<NominationPool> { pool -> knownNovaPools.isNovaPool(chain.id, pool.id) }
        .thenByDescending { it.apy.orZero() }
        .thenByDescending { it.membersCount }
}

