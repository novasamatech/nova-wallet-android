package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.common.utils.timestamp
import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.core_db.model.TotalRewardLocal
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapTotalRewardLocalToTotalReward
import io.novafoundation.nova.feature_staking_impl.data.model.stakingExternalApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.StakingApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingSumRewardRequest
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.response.totalReward
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class SubqueryStakingRewardsDataSource(
    private val stakingApi: StakingApi,
    private val stakingTotalRewardDao: StakingTotalRewardDao,
) : StakingRewardsDataSource {

    override fun totalRewardsFlow(accountAddress: String, chainId: ChainId, chainAssetId: Int): Flow<TotalReward> {
        return stakingTotalRewardDao.observeTotalRewards(accountAddress, chainId, chainAssetId)
            .filterNotNull()
            .map(::mapTotalRewardLocalToTotalReward)
    }

    override suspend fun sync(accountAddress: String, chain: Chain, chainAsset: Chain.Asset, rewardPeriod: RewardPeriod) {
        val stakingExternalApi = chain.stakingExternalApi() ?: return
        val start = rewardPeriod.getStartDate()?.timestamp()
        val end = rewardPeriod.getEndDate()?.timestamp()

        val response = stakingApi.getTotalReward(
            url = stakingExternalApi.url,
            body = StakingSumRewardRequest(accountAddress = accountAddress, startTimestamp = start, endTimestamp = end)
        )
        val totalResult = response.data.totalReward

        val totalRewardLocal = TotalRewardLocal(
            accountAddress = accountAddress,
            chainId = chain.id,
            chainAssetId = chainAsset.id,
            totalReward = totalResult
        )

        stakingTotalRewardDao.insert(totalRewardLocal)
    }

    private fun RewardPeriod.getStartDate(): Date? {
        return when (start) {
            is RewardPeriod.TimePoint.NoThreshold -> null
            is RewardPeriod.TimePoint.Threshold -> Date(start.millis)
            is RewardPeriod.TimePoint.ThresholdOffset -> {
                val endDate = getEndDate() ?: Date()
                Date(endDate.time - this.start.millis)
            }
        }
    }

    private fun RewardPeriod.getEndDate(): Date? {
        return when (end) {
            is RewardPeriod.TimePoint.NoThreshold -> null
            is RewardPeriod.TimePoint.Threshold -> Date(end.millis)
            is RewardPeriod.TimePoint.ThresholdOffset -> throw IllegalStateException()
        }
    }
}
