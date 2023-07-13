package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.StakingRewardsDataSource
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

class StakingRewardsRepository(
    private val stakingRewardsDataSource: StakingRewardsDataSource,
) {

    fun totalRewardFlow(accountAddress: String, chainId: ChainId, chainAssetId: Int): Flow<TotalReward> {
        return stakingRewardsDataSource.totalRewardsFlow(accountAddress, chainId, chainAssetId)
    }

    suspend fun sync(accountAddress: String, chain: Chain, chainAsset: Chain.Asset, rewardPeriod: RewardPeriod) {
        stakingRewardsDataSource.sync(accountAddress, chain, chainAsset, rewardPeriod)
    }
}
