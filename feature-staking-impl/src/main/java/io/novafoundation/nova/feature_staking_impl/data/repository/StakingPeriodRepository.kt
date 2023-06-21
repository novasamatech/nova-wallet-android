package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.StakingRewardPeriodDataSource
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface StakingPeriodRepository {

    suspend fun setRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, rewardPeriod: RewardPeriod)

    suspend fun getRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset): RewardPeriod

    fun observeRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset): Flow<RewardPeriod>
}

class RealStakingPeriodRepository(
    private val stakingRewardPeriodDataSource: StakingRewardPeriodDataSource
) : StakingPeriodRepository {

    override suspend fun setRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, rewardPeriod: RewardPeriod) {
        stakingRewardPeriodDataSource.setRewardPeriod(accountId, chain, asset, rewardPeriod)
    }

    override suspend fun getRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset): RewardPeriod {
        return stakingRewardPeriodDataSource.getRewardPeriod(accountId, chain, asset)
    }

    override fun observeRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset): Flow<RewardPeriod> {
        return stakingRewardPeriodDataSource.observeRewardPeriod(accountId, chain, asset)
    }
}
