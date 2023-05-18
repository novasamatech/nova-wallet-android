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
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
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
    private val tokenRepository: TokenRepository
) : StakingDashboardInteractor {

    override fun stakingDashboardFlow(): Flow<StakingDashboard> {
        return flow {
            val chains = chainRegistry.chainsById()
            val knownStakingAssets = chains.knownStakingAssets()
            val knownStakingChainsCount = knownStakingAssets.distinctBy { it.chainId }.size

            val noPriceDashboardFlow = accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
                combine(
                    dashboardRepository.dashboardItemsFlow(metaAccount.id),
                    stakingDashboardSyncTracker.syncedItemsFlow,
                ) { dashboardItems, syncedItems ->
                    constructStakingDashboard(chains, knownStakingChainsCount, dashboardItems, syncedItems)
                }
            }

            val pricesFlow = tokenRepository.observeTokens(knownStakingAssets)

            val dashboardFlow = combine(noPriceDashboardFlow, pricesFlow, ::addPricesToDashboard)

            emitAll(dashboardFlow)
        }
    }

    private fun constructStakingDashboard(
        chainsById: ChainsById,
        knownStakingChainsCount: Int,
        dashboardItems: List<StakingDashboardItem>,
        syncedIds: Set<StakingOptionId>
    ): NoPriceStakingDashboard {
        val itemsByChain = dashboardItems.groupBy(StakingDashboardItem::fullChainAssetId)

        val hasStake = mutableListOf<NoPriceStakingDashboardOption<HasStake>>()
        val noStake = mutableListOf<NoPriceStakingDashboardOption<NoStake>>()

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

        return NoPriceStakingDashboard(
            hasStake = hasStake,
            noStake = noStake,
            resolvingItems = resolvingItems
        )
    }

    private fun addPricesToDashboard(
        noPriceStakingDashboard: NoPriceStakingDashboard,
        prices: Map<FullChainAssetId, Token>
    ): StakingDashboard {
        return StakingDashboard(
            hasStake = noPriceStakingDashboard.hasStake.map { addPriceToStakingDashboardItem(it, prices) },
            noStake = noPriceStakingDashboard.noStake.map { addPriceToStakingDashboardItem(it, prices) },
            resolvingItems = noPriceStakingDashboard.resolvingItems
        )
    }

    private fun <S> addPriceToStakingDashboardItem(
        item: NoPriceStakingDashboardOption<S>,
        prices: Map<FullChainAssetId, Token>,
    ): AggregatedStakingDashboardOption<S> {
        return AggregatedStakingDashboardOption(
            chain = item.chain,
            token = prices.getValue(item.chainAsset.fullId),
            stakingState = item.stakingState,
            syncing = item.syncing
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
    ): NoPriceStakingDashboardOption<NoStake> {
        val maxEarnings = items.findMaxEarnings()
        val stats = maxEarnings?.let(NoStake::Stats)

        return NoPriceStakingDashboardOption(
            chain = chain,
            chainAsset = chainAsset,
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
    ): NoPriceStakingDashboardOption<HasStake>? {
        if (item.stakeState !is StakingDashboardItem.StakeState.HasStake) return null

        return NoPriceStakingDashboardOption(
            chain = chain,
            chainAsset = chainAsset,
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

    private fun ChainsById.knownStakingAssets(): List<Chain.Asset> {
        return flatMap { (_, chain) -> chain.assets.filter { it.supportedStakingOptions().isNotEmpty() } }
    }

    private class NoPriceStakingDashboardOption<S>(
        val chain: Chain,
        val chainAsset: Chain.Asset,
        val stakingState: S,
        val syncing: Boolean
    )

    private class NoPriceStakingDashboard(
        val hasStake: List<NoPriceStakingDashboardOption<HasStake>>,
        val noStake: List<NoPriceStakingDashboardOption<NoStake>>,
        val resolvingItems: Int,
    )
}
