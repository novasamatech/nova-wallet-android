package io.novafoundation.nova.feature_staking_impl.data.dashboard.repository

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.fromOption
import io.novafoundation.nova.common.utils.asPercent
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.StakingDashboardDao
import io.novafoundation.nova.core_db.model.StakingDashboardAccountsView
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MultiStakingOptionIds
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardItem
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardItem.StakeState.HasStake
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardItem.StakeState.NoStake
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardOptionAccounts
import io.novafoundation.nova.feature_staking_impl.BuildConfig
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapStakingStringToStakingType
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapStakingTypeToStakingString
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow

interface StakingDashboardRepository {

    fun dashboardItemsFlow(metaAccountId: Long): Flow<List<StakingDashboardItem>>

    fun dashboardItemsFlow(metaAccountId: Long, multiStakingOptionIds: MultiStakingOptionIds): Flow<List<StakingDashboardItem>>

    fun stakingAccountsFlow(metaAccountId: Long): Flow<List<StakingDashboardOptionAccounts>>
}

class RealStakingDashboardRepository(
    private val dao: StakingDashboardDao
) : StakingDashboardRepository {

    override fun dashboardItemsFlow(metaAccountId: Long): Flow<List<StakingDashboardItem>> {
        return dao.dashboardItemsFlow(metaAccountId).mapList(::mapDashboardItemFromLocal)
    }

    override fun dashboardItemsFlow(metaAccountId: Long, multiStakingOptionIds: MultiStakingOptionIds): Flow<List<StakingDashboardItem>> {
        val stakingTypes = multiStakingOptionIds.stakingTypes.mapNotNull(::mapStakingTypeToStakingString)

        return dao.dashboardItemsFlow(metaAccountId, multiStakingOptionIds.chainId, multiStakingOptionIds.chainAssetId, stakingTypes)
            .mapList(::mapDashboardItemFromLocal)
    }

    override fun stakingAccountsFlow(metaAccountId: Long): Flow<List<StakingDashboardOptionAccounts>> {
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

    private fun mapStakingAccountViewFromLocal(localItem: StakingDashboardAccountsView): StakingDashboardOptionAccounts {
        return StakingDashboardOptionAccounts(
            stakingOptionId = StakingOptionId(
                chainId = localItem.chainId,
                chainAssetId = localItem.chainAssetId,
                stakingType = mapStakingStringToStakingType(localItem.stakingType),
            ),
            stakingStatusAccount = localItem.stakeStatusAccount?.intoKey(),
            rewardsAccount = localItem.rewardsAccount?.intoKey()
        )
    }

    private fun hasStakeState(localItem: StakingDashboardItemLocal): HasStake {
        val stakingType = mapStakingStringToStakingType(localItem.stakingType)

        // Subtensor has no offchain indexer — neither rewards nor estimated
        // earnings are persisted. iOS handles this by leaving the CoreData
        // fields nil and computing a [TEMP-TAOSTATS] 0.18 fallback at the
        // dashboard model layer (`StakingDashboardModel.swift:51-67`,
        // `#if DEBUG`). We mirror that here: substitute zero rewards and the
        // same DEBUG-only APY fallback so the row reaches a Loaded state
        // instead of a perpetual shimmer. Drop both substitutions when an
        // indexer / TaoStats data source ships.
        val rewardsRaw = localItem.rewards ?: substituteSubtensorRewards(stakingType)
        val earningsRaw = localItem.estimatedEarnings ?: substituteSubtensorApy(stakingType)
        val status = localItem.status

        val stats = if (earningsRaw != null && rewardsRaw != null && status != null) {
            HasStake.Stats(
                rewards = rewardsRaw,
                status = mapStakingStatusFromLocal(status),
                estimatedEarnings = earningsRaw.asPercent()
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
        val stakingType = mapStakingStringToStakingType(localItem.stakingType)
        val earningsRaw = localItem.estimatedEarnings ?: substituteSubtensorApy(stakingType)

        val stats = earningsRaw?.let { NoStake.Stats(it.asPercent()) }
        return NoStake(ExtendedLoadingState.fromOption(stats))
    }

    /**
     * Substitutes a non-null rewards value for SUBTENSOR rows. iOS leaves the
     * field nil; Android's `HasStake.Stats.rewards` is non-nullable, so zero
     * is the closest equivalent.
     */
    private fun substituteSubtensorRewards(stakingType: Chain.Asset.StakingType): BigInteger? =
        BigInteger.ZERO.takeIf { stakingType == Chain.Asset.StakingType.SUBTENSOR }

    /**
     * [TEMP-TAOSTATS] 0.18 (18%) APY fallback for SUBTENSOR rows in DEBUG only.
     * Mirrors iOS `StakingDashboardModel.swift:Concrete.maxApy` — same value
     * the iOS Start Staking info screen uses. In release builds we leave this
     * null to match iOS release semantics (returns nil, view-layer handles).
     */
    private fun substituteSubtensorApy(stakingType: Chain.Asset.StakingType): Double? =
        if (BuildConfig.DEBUG && stakingType == Chain.Asset.StakingType.SUBTENSOR) {
            SUBTENSOR_TEMP_TAOSTATS_FALLBACK_APY
        } else {
            null
        }

    companion object {
        private const val SUBTENSOR_TEMP_TAOSTATS_FALLBACK_APY = 0.18
    }

    private fun mapStakingStatusFromLocal(localStatus: StakingDashboardItemLocal.Status): HasStake.StakingStatus {
        return when (localStatus) {
            StakingDashboardItemLocal.Status.ACTIVE -> HasStake.StakingStatus.ACTIVE
            StakingDashboardItemLocal.Status.INACTIVE -> HasStake.StakingStatus.INACTIVE
            StakingDashboardItemLocal.Status.WAITING -> HasStake.StakingStatus.WAITING
        }
    }
}
