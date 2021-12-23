package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface StakingRewardsDataSource {
    fun totalRewardsFlow(accountAddress: String): Flow<TotalReward>

    suspend fun sync(accountAddress: String, chain: Chain)
}
