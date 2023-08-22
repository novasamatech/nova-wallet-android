package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation

import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool

interface NominationPoolRecommendator {

    fun recommendedPool(): NominationPool
}
