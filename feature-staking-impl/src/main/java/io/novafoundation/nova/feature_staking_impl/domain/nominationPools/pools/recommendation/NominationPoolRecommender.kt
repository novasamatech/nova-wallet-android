package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation

import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool

interface NominationPoolRecommender {

    val recommendations: List<NominationPool>

    fun recommendedPool(): NominationPool?
}
