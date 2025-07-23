package io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward

import io.novafoundation.nova.common.utils.atTheBeginningOfTheDay
import io.novafoundation.nova.common.utils.atTheEndOfTheDay
import io.novafoundation.nova.common.utils.timestamp
import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.core_db.model.TotalRewardLocal
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.mappers.mapTotalRewardLocalToTotalReward
import io.novafoundation.nova.feature_staking_impl.data.model.stakingExternalApi
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.timelineChainId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapStakingTypeToStakingString
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

abstract class BaseStakingRewardsDataSource(
    private val chainRegistry: ChainRegistry,
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

    override suspend fun sync(accountId: AccountId, stakingOption: StakingOption, rewardPeriod: RewardPeriod) {
        val chain = stakingOption.assetWithChain.chain
        val timelineChain = chain.timelineChainId()?.let { chainRegistry.getChain(it) }

        val totalReward = if (timelineChain != null) {
            // We aggregate rewards for chains that have migrated their features to AH
            getRewardSumForChains(listOfNotNull(chain, timelineChain), accountId, rewardPeriod)
        } else {
            getRewardSumForChains(listOf(chain), accountId, rewardPeriod)
        }

        saveTotalReward(totalReward, accountId, stakingOption)
    }

    private suspend fun getRewardSumForChains(chains: List<Chain>, accountId: AccountId, rewardPeriod: RewardPeriod): Balance {
        return chains.sumOf { chain ->
            getTotalRewards(chain, accountId, rewardPeriod)
        }
    }

    abstract suspend fun getTotalRewards(chain: Chain, accountId: AccountId, rewardPeriod: RewardPeriod): Balance

    protected val RewardPeriod.startTimestamp: Long?
        get() = start?.atTheBeginningOfTheDay()?.timestamp() // Using atTheBeginningOfTheDay() to avoid invalid data

    protected val RewardPeriod.endTimestamp: Long?
        get() = end?.atTheEndOfTheDay()?.timestamp() // Using atTheEndOfTheDay() since the end of the day is fully included in the period
}
