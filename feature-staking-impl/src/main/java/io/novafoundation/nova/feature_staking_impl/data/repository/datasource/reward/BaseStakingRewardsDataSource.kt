package io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward

import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.core_db.model.TotalRewardLocal
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapTotalRewardLocalToTotalReward
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapStakingTypeToStakingString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

abstract class BaseStakingRewardsDataSource(
    private val stakingTotalRewardDao: StakingTotalRewardDao,
) : StakingRewardsDataSource {

    override fun totalRewardsFlow(accountId: AccountId, stakingOptionId: StakingOptionId): Flow<TotalReward> {
        val stakingTypeRaw = mapStakingTypeToStakingString(stakingOptionId.stakingType) ?: return emptyFlow()

        return stakingTotalRewardDao.observeTotalRewards(accountId, stakingOptionId.chainId, stakingOptionId.chainAssetId, stakingTypeRaw)
            .filterNotNull()
            .map(::mapTotalRewardLocalToTotalReward)
    }

    protected suspend fun saveTotalReward(totalReward: Balance, accountId: AccountId, stakingOption: StakingOption) {
        val stakingTypeRaw = mapStakingTypeToStakingString(stakingOption.additional.stakingType) ?: return

        val totalRewardLocal = TotalRewardLocal(
            accountId = accountId,
            chainId = stakingOption.assetWithChain.chain.id,
            chainAssetId = stakingOption.assetWithChain.asset.id,
            stakingType = stakingTypeRaw,
            totalReward = totalReward
        )

        stakingTotalRewardDao.insert(totalRewardLocal)
    }
}
