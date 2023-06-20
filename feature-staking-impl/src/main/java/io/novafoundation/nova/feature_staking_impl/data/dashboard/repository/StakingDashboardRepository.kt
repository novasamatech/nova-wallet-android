package io.novafoundation.nova.feature_staking_impl.data.dashboard.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.fromOption
import io.novafoundation.nova.common.utils.asPercent
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.StakingDashboardDao
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.core_db.model.StakingDashboardPrimaryAccountView
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardItem
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardItem.StakeState.HasStake
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardItem.StakeState.NoStake
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardPrimaryAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapStakingStringToStakingType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow

interface StakingDashboardRepository {

    fun dashboardItemsFlow(metaAccountId: Long): Flow<List<StakingDashboardItem>>

    fun stakingAccountsFlow(metaAccountId: Long): Flow<List<StakingDashboardPrimaryAccount>>
}

class RealStakingDashboardRepository(
    private val dao: StakingDashboardDao
) : StakingDashboardRepository {

    override fun dashboardItemsFlow(metaAccountId: Long): Flow<List<StakingDashboardItem>> {
        return dao.dashboardItemsFlow(metaAccountId).mapList(::mapDashboardItemFromLocal)
    }

    override fun stakingAccountsFlow(metaAccountId: Long): Flow<List<StakingDashboardPrimaryAccount>> {
        return dao.stakingAccountsViewFlow(metaAccountId).mapList(::mapStakingAccountViewFromLocal)
    }

    private fun mapDashboardItemFromLocal(localItem: StakingDashboardItemLocal): StakingDashboardItem {
        return StakingDashboardItem(
            fullChainAssetId = FullChainAssetId(
                chainId = localItem.chainId,
                assetId = localItem.chainAssetId,
            ),
            stakingType = mapStakingStringToStakingType(localItem.stakingType),
            stakeState = if (localItem.hasStake) hasStakeState(localItem) else noStakeState(localItem)
        )
    }

    private fun mapStakingAccountViewFromLocal(localItem: StakingDashboardPrimaryAccountView): StakingDashboardPrimaryAccount {
        return StakingDashboardPrimaryAccount(
            stakingOptionId = StakingOptionId(
                chainId = localItem.chainId,
                chainAssetId = localItem.chainAssetId,
                stakingType = mapStakingStringToStakingType(localItem.stakingType),
            ),
            primaryStakingAccountId = localItem.primaryStakingAccountId?.let(::AccountIdKey)
        )
    }

    private fun hasStakeState(localItem: StakingDashboardItemLocal): HasStake {
        val estimatedEarnings = localItem.estimatedEarnings
        val rewards = localItem.rewards
        val status = localItem.status

        val stats = if (estimatedEarnings != null && rewards != null && status != null) {
            HasStake.Stats(
                rewards = rewards,
                status = mapStakingStatusFromLocal(status),
                estimatedEarnings = estimatedEarnings.asPercent()
            )
        } else {
            null
        }

        return HasStake(
            stake = requireNotNull(localItem.stake),
            stats = ExtendedLoadingState.fromOption(stats)
        )
    }

    private fun noStakeState(localItem: StakingDashboardItemLocal): NoStake {
        val stats = localItem.estimatedEarnings?.let { estimatedEarnings ->
            NoStake.Stats(estimatedEarnings.asPercent())
        }

        return NoStake(ExtendedLoadingState.fromOption(stats))
    }

    private fun mapStakingStatusFromLocal(localStatus: StakingDashboardItemLocal.Status): HasStake.StakingStatus {
        return when (localStatus) {
            StakingDashboardItemLocal.Status.ACTIVE -> HasStake.StakingStatus.ACTIVE
            StakingDashboardItemLocal.Status.INACTIVE -> HasStake.StakingStatus.INACTIVE
            StakingDashboardItemLocal.Status.WAITING -> HasStake.StakingStatus.WAITING
        }
    }
}
