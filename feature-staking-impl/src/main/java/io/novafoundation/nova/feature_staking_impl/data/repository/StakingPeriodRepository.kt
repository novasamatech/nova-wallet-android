package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.StakingRewardPeriodDataSource
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface StakingPeriodRepository {

    suspend fun setRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, stakingType: Chain.Asset.StakingType, rewardPeriod: RewardPeriod)

    suspend fun getRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, stakingType: Chain.Asset.StakingType): RewardPeriod

    fun observeRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, stakingType: Chain.Asset.StakingType): Flow<RewardPeriod>
}

class RealStakingPeriodRepository(
    private val stakingRewardPeriodDataSource: StakingRewardPeriodDataSource
) : StakingPeriodRepository {

    override suspend fun setRewardPeriod(
        accountId: AccountId,
        chain: Chain,
        asset: Chain.Asset,
        stakingType: Chain.Asset.StakingType,
        rewardPeriod: RewardPeriod
    ) {
        stakingRewardPeriodDataSource.setRewardPeriod(accountId, chain, asset, stakingType, rewardPeriod)
    }

    override suspend fun getRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, stakingType: Chain.Asset.StakingType): RewardPeriod {
        return stakingRewardPeriodDataSource.getRewardPeriod(accountId, chain, asset, stakingType)
    }

    override fun observeRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, stakingType: Chain.Asset.StakingType): Flow<RewardPeriod> {
        return stakingRewardPeriodDataSource.observeRewardPeriod(accountId, chain, asset, stakingType)
    }
}
