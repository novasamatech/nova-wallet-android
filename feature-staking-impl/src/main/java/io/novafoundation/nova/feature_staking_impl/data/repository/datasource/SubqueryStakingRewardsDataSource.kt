package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.core_db.model.TotalRewardLocal
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapSubqueryHistoryToTotalReward
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapTotalRewardLocalToTotalReward
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.StakingApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingSumRewardRequest
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class SubqueryStakingRewardsDataSource(
    private val stakingApi: StakingApi,
    private val stakingTotalRewardDao: StakingTotalRewardDao,
) : StakingRewardsDataSource {

    override fun totalRewardsFlow(accountAddress: String): Flow<TotalReward> {
        return stakingTotalRewardDao.observeTotalRewards(accountAddress)
            .filterNotNull()
            .map(::mapTotalRewardLocalToTotalReward)
    }

    override suspend fun sync(accountAddress: String, chain: Chain) {
        val stakingExternalApi = chain.externalApi?.staking ?: return

        val totalReward = mapSubqueryHistoryToTotalReward(
            stakingApi.getSumReward(
                url = stakingExternalApi.url,
                body = StakingSumRewardRequest(accountAddress = accountAddress)
            )
        )

        stakingTotalRewardDao.insert(TotalRewardLocal(accountAddress, totalReward))
    }
}
