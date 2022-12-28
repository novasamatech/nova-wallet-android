package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.core_db.model.TotalRewardLocal
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapTotalRewardLocalToTotalReward
import io.novafoundation.nova.feature_staking_impl.data.model.stakingExternalApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.StakingApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.request.StakingSumRewardRequest
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.response.totalReward
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
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

    override suspend fun sync(accountAddress: String, chain: Chain, chainAsset: Chain.Asset) {
        val stakingExternalApi = chain.stakingExternalApi() ?: return

        val response = stakingApi.getTotalReward(
            url = stakingExternalApi.url,
            body = StakingSumRewardRequest(accountAddress = accountAddress)
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
}
