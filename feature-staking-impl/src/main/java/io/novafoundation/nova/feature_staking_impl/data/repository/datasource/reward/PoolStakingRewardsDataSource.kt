package io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward

import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.model.stakingExternalApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.StakingApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.PoolStakingPeriodRewardsRequest
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.response.totalReward
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.runtime.ext.addressOf
import io.novasama.substrate_sdk_android.runtime.AccountId

class PoolStakingRewardsDataSource(
    private val stakingApi: StakingApi,
    stakingTotalRewardDao: StakingTotalRewardDao,
) : BaseStakingRewardsDataSource(stakingTotalRewardDao) {

    override suspend fun sync(accountId: AccountId, stakingOption: StakingOption, rewardPeriod: RewardPeriod) {
        val chain = stakingOption.assetWithChain.chain

        val stakingExternalApi = chain.stakingExternalApi() ?: return
        val address = chain.addressOf(accountId)

        val response = stakingApi.getPoolRewardsByPeriod(
            url = stakingExternalApi.url,
            body = PoolStakingPeriodRewardsRequest(
                accountAddress = address,
                startTimestamp = rewardPeriod.startTimestamp,
                endTimestamp = rewardPeriod.endTimestamp
            )
        )
        val totalResult = response.data.totalReward

        saveTotalReward(totalResult, accountId, stakingOption)
    }
}
