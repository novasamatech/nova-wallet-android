package io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward

import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.feature_staking_impl.data.model.stakingRewardsExternalApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.StakingApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.DirectStakingPeriodRewardsRequest
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

class DirectStakingRewardsDataSource(
    private val stakingApi: StakingApi,
    stakingTotalRewardDao: StakingTotalRewardDao,
) : BaseStakingRewardsDataSource(stakingTotalRewardDao) {

    override suspend fun getTotalRewards(chain: Chain, accountId: AccountId, rewardPeriod: RewardPeriod): Balance {
        val stakingExternalApis = chain.stakingRewardsExternalApi()
        val address = chain.addressOf(accountId)

        return getAggregatedRewards(stakingExternalApis) { url ->
            stakingApi.getRewardsByPeriod(
                url = url,
                body = DirectStakingPeriodRewardsRequest(
                    accountAddress = address,
                    startTimestamp = rewardPeriod.startTimestamp,
                    endTimestamp = rewardPeriod.endTimestamp
                )
            )
        }
    }
}
