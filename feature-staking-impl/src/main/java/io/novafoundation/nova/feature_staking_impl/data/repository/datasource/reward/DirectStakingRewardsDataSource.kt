package io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward

import io.novafoundation.nova.common.utils.atTheBeginningOfTheDay
import io.novafoundation.nova.common.utils.atTheEndOfTheDay
import io.novafoundation.nova.common.utils.timestamp
import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.model.stakingExternalApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.StakingApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingPeriodRewardsRequest
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.response.totalReward
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.runtime.ext.addressOf
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class DirectStakingRewardsDataSource(
    private val stakingApi: StakingApi,
    private val stakingTotalRewardDao: StakingTotalRewardDao,
) : BaseStakingRewardsDataSource(stakingTotalRewardDao) {

    override suspend fun sync(accountId: AccountId, stakingOption: StakingOption, rewardPeriod: RewardPeriod) {
        val chain = stakingOption.assetWithChain.chain

        val stakingExternalApi = chain.stakingExternalApi() ?: return
        val address = chain.addressOf(accountId)

        val start = rewardPeriod.start?.atTheBeginningOfTheDay() // Using atTheBeginningOfTheDay() to avoid invalid data
            ?.timestamp()
        val end = rewardPeriod.end?.atTheEndOfTheDay() // Using atTheEndOfTheDay() since the end of the day is fully included in the period
            ?.timestamp()

        val response = stakingApi.getRewardsByPeriod(
            url = stakingExternalApi.url,
            body = StakingPeriodRewardsRequest(accountAddress = address, startTimestamp = start, endTimestamp = end)
        )
        val totalResult = response.data.totalReward

        saveTotalReward(totalResult, accountId, stakingOption)
    }
}
