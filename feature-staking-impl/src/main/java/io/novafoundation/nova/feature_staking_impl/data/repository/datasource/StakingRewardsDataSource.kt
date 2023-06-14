package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface StakingRewardsDataSource {

    fun totalRewardsFlow(
        accountAddress: String,
        chainId: ChainId,
        chainAssetId: Int
    ): Flow<TotalReward>

    suspend fun sync(accountAddress: String, chain: Chain, chainAsset: Chain.Asset)

    suspend fun sync(accountAddress: String, chain: Chain, chainAsset: Chain.Asset, rewardPeriod: RewardPeriod)

    suspend fun clearRewards()
}
