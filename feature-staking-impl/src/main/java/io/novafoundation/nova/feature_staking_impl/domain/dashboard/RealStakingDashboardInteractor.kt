@file:OptIn(ExperimentalCoroutinesApi::class)

package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.fromOption
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardSyncTracker
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardItem
import io.novafoundation.nova.feature_staking_impl.data.dashboard.repository.StakingDashboardRepository
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class RealStakingDashboardInteractor(
    private val dashboardRepository: StakingDashboardRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val stakingDashboardSyncTracker: StakingDashboardSyncTracker,
) : StakingDashboardInteractor {

    override fun stakingDashboardFlow(): Flow<StakingDashboard> {
        return flow {
            val chains = chainRegistry.chainsById()
            val knownStakingChainsCount = chains.knownStakingChainsCount()

            val innerFlow = accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
                combine(
                    dashboardRepository.dashboardItemsFlow(metaAccount.id),
                    stakingDashboardSyncTracker.syncedItemsFlow,
                ) { dashboardItems, syncedItems ->
                    constructStakingDashboard(chains, knownStakingChainsCount, dashboardItems, syncedItems)
                }
            }

            emitAll(innerFlow)
        }
    }

    private fun constructStakingDashboard(
        chainsById: ChainsById,
        knownStakingChainsCount: Int,
        dashboardItems: List<StakingDashboardItem>,
        syncedIds: Set<StakingOptionId>
    ): StakingDashboard {
        val itemsByChain = dashboardItems.groupBy(StakingDashboardItem::fullChainAssetId)

        val hasStake = mutableListOf<AggregatedStakingDashboardOption<HasStake>>()
        val noStake = mutableListOf<AggregatedStakingDashboardOption<NoStake>>()

        itemsByChain.forEach { (fullChainAssetId, items) ->
            val chain = chainsById[fullChainAssetId.chainId] ?: return@forEach
            val asset = chain.assetsById[fullChainAssetId.assetId] ?: return@forEach

            if (items.isNoStakePresent()) {
                noStake.add(noStakeAggregatedOption(chain, asset, items, syncedIds))
            } else {
                val hasStakeOptions = items.mapNotNull { item -> hasStakeOption(chain, asset, item, syncedIds) }
                hasStake.addAll(hasStakeOptions)
            }
        }

        val resolvedItems = itemsByChain.size
        val resolvingItems = (knownStakingChainsCount - resolvedItems).coerceAtLeast(0)

        return StakingDashboard(
            hasStake = hasStake,
            noStake = noStake,
            resolvingItems = resolvingItems
        )
    }

    private fun List<StakingDashboardItem>.isNoStakePresent() = all { it.stakeState is StakingDashboardItem.StakeState.NoStake }

    private fun List<StakingDashboardItem>.findMaxEarnings(): Percent? = mapNotNull {
        it.stakeState.stats.dataOrNull?.estimatedEarnings
    }.maxOrNull()

    private fun noStakeAggregatedOption(
        chain: Chain,
        chainAsset: Chain.Asset,
        items: List<StakingDashboardItem>,
        syncedIds: Set<StakingOptionId>,
    ): AggregatedStakingDashboardOption<NoStake> {
        val maxEarnings = items.findMaxEarnings()
        val stats = maxEarnings?.let(NoStake::Stats)

        return AggregatedStakingDashboardOption(
            chain = chain,
            stakingState = NoStake(
                stats = ExtendedLoadingState.fromOption(stats),
                flowType = NoStake.FlowType.Aggregated
            ),
            syncing = chainAsset.supportedStakingOptions().any { stakingType ->
                StakingOptionId(chain.id, chainAsset.id, stakingType) !in syncedIds
            }
        )
    }

    private fun hasStakeOption(
        chain: Chain,
        chainAsset: Chain.Asset,
        item: StakingDashboardItem,
        syncedIds: Set<StakingOptionId>,
    ): AggregatedStakingDashboardOption<HasStake>? {
        if (item.stakeState !is StakingDashboardItem.StakeState.HasStake) return null

        return AggregatedStakingDashboardOption(
            chain = chain,
            stakingState = HasStake(
                stats = item.stakeState.stats.map(::mapItemStatsToOptionStats),
                stakingType = item.stakingType,
                stake = item.stakeState.stake
            ),
            syncing = StakingOptionId(chain.id, chainAsset.id, item.stakingType) !in syncedIds
        )
    }

    private fun mapItemStatsToOptionStats(itemStats: StakingDashboardItem.StakeState.HasStake.Stats): HasStake.Stats {
        return HasStake.Stats(
            rewards = itemStats.rewards,
            estimatedEarnings = itemStats.estimatedEarnings,
            status = mapItemStatusToOptionStatus(itemStats.status)
        )
    }

    private fun mapItemStatusToOptionStatus(itemStatus: StakingDashboardItem.StakeState.HasStake.StakingStatus): HasStake.StakingStatus {
        return when (itemStatus) {
            StakingDashboardItem.StakeState.HasStake.StakingStatus.ACTIVE -> HasStake.StakingStatus.ACTIVE
            StakingDashboardItem.StakeState.HasStake.StakingStatus.INACTIVE -> HasStake.StakingStatus.INACTIVE
            StakingDashboardItem.StakeState.HasStake.StakingStatus.WAITING -> HasStake.StakingStatus.WAITING
        }
    }

    private fun ChainsById.knownStakingChainsCount(): Int {
        return count { (_, chain) -> chain.assets.any { it.supportedStakingOptions().isNotEmpty() } }
    }
}
