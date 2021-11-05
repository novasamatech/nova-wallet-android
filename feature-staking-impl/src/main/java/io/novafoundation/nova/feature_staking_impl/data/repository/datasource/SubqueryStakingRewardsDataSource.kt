package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.common.utils.networkType
import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.core_db.model.TotalRewardLocal
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapSubqueryHistoryToTotalReward
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapTotalRewardLocalToTotalReward
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.StakingApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingSumRewardRequest
import io.novafoundation.nova.feature_staking_impl.data.repository.subqueryNovaApiPath
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class SubqueryStakingRewardsDataSource(
    private val stakingApi: StakingApi,
    private val stakingTotalRewardDao: StakingTotalRewardDao,
) : StakingRewardsDataSource {

    override suspend fun totalRewardsFlow(accountAddress: String): Flow<TotalReward> {
        return stakingTotalRewardDao.observeTotalRewards(accountAddress)
            .filterNotNull()
            .map(::mapTotalRewardLocalToTotalReward)
    }

    override suspend fun sync(accountAddress: String) {
        val subqueryPath = accountAddress.networkType().subqueryNovaApiPath()

        val totalReward = mapSubqueryHistoryToTotalReward(
            stakingApi.getSumReward(
                subqueryPath,
                StakingSumRewardRequest(accountAddress = accountAddress)
            )
        )

        stakingTotalRewardDao.insert(TotalRewardLocal(accountAddress, totalReward))
    }
}
