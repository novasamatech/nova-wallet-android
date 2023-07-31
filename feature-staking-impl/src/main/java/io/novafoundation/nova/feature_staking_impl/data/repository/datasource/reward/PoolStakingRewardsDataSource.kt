package io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward

import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.StakingApi
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class PoolStakingRewardsDataSource(
    private val stakingApi: StakingApi,
    stakingTotalRewardDao: StakingTotalRewardDao,
) : BaseStakingRewardsDataSource(stakingTotalRewardDao) {

    override suspend fun sync(accountId: AccountId, stakingOption: StakingOption, rewardPeriod: RewardPeriod) {
        // TODO sync pool rewards when subQuery is ready
        saveTotalReward(Balance.ZERO, accountId, stakingOption)
    }
}
