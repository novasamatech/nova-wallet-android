package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.pools.recommendation.NominationPoolRecommenderFactory
import kotlinx.coroutines.CoroutineScope

class SelectingNominationPoolInteractor(
    private val nominationPoolRecommenderFactory: NominationPoolRecommenderFactory
) {

    suspend fun getSortedNominationPools(stakingOption: StakingOption, coroutineScope: CoroutineScope): List<NominationPool> {
        return nominationPoolRecommenderFactory.create(stakingOption, coroutineScope)
            .recommendations
    }
}
