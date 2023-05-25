@file:OptIn(ExperimentalCoroutinesApi::class)

package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.fromOption
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.model.isStaking
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardSyncTracker
import io.novafoundation.nova.feature_staking_api.data.dashboard.SyncingStageMap
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.SyncingStage
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MoreStakingOptions
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDApp
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardItem
import io.novafoundation.nova.feature_staking_impl.data.dashboard.repository.StakingDashboardRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RealStakingDashboardInteractor(
    private val dashboardRepository: StakingDashboardRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val stakingDashboardSyncTracker: StakingDashboardSyncTracker,
    private val dAppMetadataRepository: DAppMetadataRepository,
    private val walletRepository: WalletRepository,
) : StakingDashboardInteractor {
    override suspend fun syncDapps() {
        runCatching {
            withContext(Dispatchers.Default) {
                dAppMetadataRepository.syncDAppMetadatas()
            }
        }
    }

    override fun stakingDashboardFlow(): Flow<StakingDashboard> {
        return flow {
            val chains = chainRegistry.chainsById()
            val knownStakingAssetsByChain = chains.knownStakingAssetsByChain()
            val stakingChains = chains.keep
            val knownStakingChainsCount = knownStakingAssetsByChain.distinctBy { it.chainId }.size

            emit(StakingDashboard(hasStake = emptyList(), noStake = emptyList(), resolvingItems = knownStakingChainsCount))

            val dashboardFlow = accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
                val noPriceDashboardFlow = combine(
                    dashboardRepository.dashboardItemsFlow(metaAccount.id),
                    stakingDashboardSyncTracker.syncedItemsFlow,
                ) { dashboardItems, syncedItems ->
                    constructStakingDashboard(chains, knownStakingChainsCount, dashboardItems, syncedItems)
                }

                val assetsFlow = walletRepository.supportAssetsByIdFlow(metaAccount.id, knownStakingAssetsByChain)

                combine(noPriceDashboardFlow, assetsFlow, ::addPricesToDashboard)
            }

            emitAll(dashboardFlow)
        }
    }

    override fun moreStakingOptionsFlow(): Flow<MoreStakingOptions> {
        return flow {
            val chains = chainRegistry.chainsById()
            val knownStakingAssets = chains.knownStakingAssetsByChain()
            val knownStakingChainsCount = knownStakingAssets.distinctBy { it.chainId }.size
            val metaAccount = accountRepository.getSelectedMetaAccount()

            val noPriceDashboardFlow = combine(
                dashboardRepository.dashboardItemsFlow(metaAccount.id),
                stakingDashboardSyncTracker.syncedItemsFlow,
            ) { dashboardItems, syncedItems ->
                constructMoreStakingOptions(chains, knownStakingChainsCount, dashboardItems, syncedItems)
            }

            val assetsFlow = walletRepository.supportAssetsByIdFlow(metaAccount.id, knownStakingAssets)
            val dApps = dAppMetadataRepository.stakingDAppsFlow()

            val dashboardFlow = combine(
                noPriceDashboardFlow,
                assetsFlow,
                dApps,
                ::combineNoMoreOptionsInfo
            )

            emitAll(dashboardFlow)
        }
    }

    private fun WalletRepository.supportAssetsByIdFlow(metaId: Long, chainAssets: List<Chain.Asset>): Flow<Map<FullChainAssetId, Asset>> {
        return supportedAssetsFlow(metaId, chainAssets)
            .map { assets -> assets.associateBy { asset -> asset.token.configuration.fullId } }
    }

    private fun constructStakingDashboard(
        chainsById: ChainsById,
        knownStakingChainsCount: Int,
        dashboardItems: List<StakingDashboardItem>,
        syncingStageMap: SyncingStageMap
    ): NoPriceStakingDashboard {
        val itemsByChain = dashboardItems.groupBy(StakingDashboardItem::fullChainAssetId)

        val hasStake = mutableListOf<NoPriceStakingDashboardOption<HasStake>>()
        val noStake = mutableListOf<NoPriceStakingDashboardOption<NoBalanceNoStake>>()

        itemsByChain.forEach { (fullChainAssetId, items) ->
            val chain = chainsById[fullChainAssetId.chainId] ?: return@forEach
            val asset = chain.assetsById[fullChainAssetId.assetId] ?: return@forEach

            if (items.isNoStakePresent()) {
                if (!chain.isTestNet) {
                    noStake.add(noStakeAggregatedOption(chain, asset, items, syncingStageMap))
                }
            } else {
                val hasStakeOptions = items.mapNotNull { item -> hasStakeOption(chain, asset, item, syncingStageMap) }
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

    private fun constructMoreStakingOptions(
        chainsById: ChainsById,
        knownStakingChainsCount: Int,
        dashboardItems: List<StakingDashboardItem>,
        syncedIds: Set<StakingOptionId>,
    ): NoPriceMoreStakingOptions {
        val itemsByChain = dashboardItems.groupBy(StakingDashboardItem::fullChainAssetId)

        val inAppStaking = mutableListOf<NoPriceStakingDashboardOption<NoBalanceNoStake>>()

        itemsByChain.forEach { (fullChainAssetId, items) ->
            val chain = chainsById[fullChainAssetId.chainId] ?: return@forEach
            val asset = chain.assetsById[fullChainAssetId.assetId] ?: return@forEach

            if (items.isNoStakePresent()) {
                if (chain.isTestNet) {
                    inAppStaking.add(noStakeAggregatedOption(chain, asset, items, syncedIds))
                }
            } else {
                val separateNoStakeOptions = items.filter { it.stakeState is StakingDashboardItem.StakeState.NoStake }
                    .map { noStakeSeparateOption(chain, asset, it, syncedIds) }

                inAppStaking.addAll(separateNoStakeOptions)
            }
        }

        val resolvedItems = itemsByChain.size
        val resolvingItems = (knownStakingChainsCount - resolvedItems).coerceAtLeast(0)

        return NoPriceMoreStakingOptions(inAppStaking, resolvingItems)
    }

    private fun addPricesToDashboard(
        noPriceStakingDashboard: NoPriceStakingDashboard,
        assets: Map<FullChainAssetId, Asset>,
    ): StakingDashboard {
        return StakingDashboard(
            hasStake = noPriceStakingDashboard.hasStake.map { addPriceToHasStakeItem(it, assets) }.sortedByChain(),
            noStake = noPriceStakingDashboard.noStake.map { addAssetInfoToNoStakeItem(it, assets) }.sortedByChain(),
            resolvingItems = noPriceStakingDashboard.resolvingItems
        )
    }

    private fun combineNoMoreOptionsInfo(
        noPriceMoreStakingOptions: NoPriceMoreStakingOptions,
        assets: Map<FullChainAssetId, Asset>,
        stakingDapps: ExtendedLoadingState<List<StakingDApp>>,
    ): MoreStakingOptions {
        return MoreStakingOptions(
            inAppStaking = noPriceMoreStakingOptions.inAppStaking.map { addAssetInfoToNoStakeItem(it, assets) },
            resolvingInAppItems = noPriceMoreStakingOptions.resolvingItems,
            browserStaking = stakingDapps
        )
    }

    private fun addPriceToHasStakeItem(
        item: NoPriceStakingDashboardOption<HasStake>,
        assets: Map<FullChainAssetId, Asset>,
    ): AggregatedStakingDashboardOption<HasStake> {
        return AggregatedStakingDashboardOption(
            chain = item.chain,
            token = assets.getValue(item.chainAsset.fullId).token,
            stakingState = item.stakingState,
            syncingStage = item.syncingStage
        )
    }

    private fun addAssetInfoToNoStakeItem(
        item: NoPriceStakingDashboardOption<NoBalanceNoStake>,
        assets: Map<FullChainAssetId, Asset>,
    ): AggregatedStakingDashboardOption<NoStake> {
        val asset = assets.getValue(item.chainAsset.fullId)

        return AggregatedStakingDashboardOption(
            chain = item.chain,
            token = asset.token,
            stakingState = NoStake(
                stats = item.stakingState.stats,
                flowType = item.stakingState.flowType,
                availableBalance = asset.availableBalanceForStakingFor(item.stakingState.flowType)
            ),
            syncingStage = item.syncingStage
        )
    }

    private fun List<StakingDashboardItem>.isNoStakePresent() = all { it.stakeState is StakingDashboardItem.StakeState.NoStake }

    private fun List<StakingDashboardItem>.findMaxEarnings(): Percent? = mapNotNull {
        it.stakeState.stats.dataOrNull?.estimatedEarnings
    }.maxOrNull()

    private fun noStakeAggregatedOption(
        chain: Chain,
        chainAsset: Chain.Asset,
        noStakeItems: List<StakingDashboardItem>,
        syncedIds: Set<StakingOptionId>,
    ): NoPriceStakingDashboardOption<NoBalanceNoStake> {
        val maxEarnings = noStakeItems.findMaxEarnings()
        val stats = maxEarnings?.let(NoStake::Stats)

        val flowType = if (noStakeItems.size > 1) {
            NoStake.FlowType.Aggregated(noStakeItems.map { it.stakingType })
        } else {
            NoStake.FlowType.Single(noStakeItems.single().stakingType)
        }

        return NoPriceStakingDashboardOption(
            chain = chain,
            chainAsset = chainAsset,
            stakingState = NoBalanceNoStake(
                stats = ExtendedLoadingState.fromOption(stats),
                flowType = flowType
            ),
            syncingStage = chainAsset.supportedStakingOptions().any { stakingType ->
                StakingOptionId(chain.id, chainAsset.id, stakingType) !in syncedIds
            }
        )
    }

    private fun noStakeSeparateOption(
        chain: Chain,
        chainAsset: Chain.Asset,
        noStakeItem: StakingDashboardItem,
        syncedIds: Set<StakingOptionId>,
    ): NoPriceStakingDashboardOption<NoBalanceNoStake> {
        val stats = noStakeItem.stakeState.stats.map {
            NoStake.Stats(it.estimatedEarnings)
        }

        return NoPriceStakingDashboardOption(
            chain = chain,
            chainAsset = chainAsset,
            stakingState = NoBalanceNoStake(
                stats = stats,
                flowType = NoStake.FlowType.Single(noStakeItem.stakingType)
            ),
            syncingStage = StakingOptionId(chain.id, chainAsset.id, noStakeItem.stakingType) !in syncedIds
        )
    }

    private fun <S> List<AggregatedStakingDashboardOption<S>>.sortedByChain(): List<AggregatedStakingDashboardOption<S>> {
        return sortedWith(Chain.defaultComparatorFrom { it.chain })
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
            syncingStage = StakingOptionId(chain.id, chainAsset.id, item.stakingType) !in syncedIds
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

    private fun ChainsById.knownStakingAssetsByChain(): Map<ChainId, List<Chain.Asset>> {
        return mapValues { (_, chain) -> chain.assets.filter { it.supportedStakingOptions().isNotEmpty() } }
    }

    private fun DAppMetadataRepository.stakingDAppsFlow(): Flow<ExtendedLoadingState<List<StakingDApp>>> {
        return observeDAppCatalog().map { dappCatalog ->
            dappCatalog.dApps
                .filter { dApp -> dApp.categories.any(DappCategory::isStaking) }
                .map { StakingDApp(it.url, it.iconLink, it.name) }
        }.withSafeLoading()
    }

    private fun Asset.availableBalanceForStakingFor(flowType: NoStake.FlowType): Balance {
        return when (flowType) {
            is NoStake.FlowType.Aggregated -> flowType.stakingTypes.maxOf { availableBalanceForStakingFor(it) }

            is NoStake.FlowType.Single -> availableBalanceForStakingFor(flowType.stakingType)
        }
    }

    private fun Asset.availableBalanceForStakingFor(stakingType: Chain.Asset.StakingType): Balance {
        // assumes account has no stake
        return when (stakingType.group()) {
            StakingTypeGroup.RELAYCHAIN -> freeInPlanks
            StakingTypeGroup.PARACHAIN -> freeInPlanks
            StakingTypeGroup.UNSUPPORTED -> Balance.ZERO
        }
    }

    private class NoPriceStakingDashboardOption<S>(
        val chain: Chain,
        val chainAsset: Chain.Asset,
        val stakingState: S,
        val syncingStage: SyncingStage
    )

    private class NoPriceStakingDashboard(
        val hasStake: List<NoPriceStakingDashboardOption<HasStake>>,
        val noStake: List<NoPriceStakingDashboardOption<NoBalanceNoStake>>,
        val resolvingItems: Int,
    )

    private class NoBalanceNoStake(
        val stats: ExtendedLoadingState<NoStake.Stats>,
        val flowType: NoStake.FlowType
    )

    private class NoPriceMoreStakingOptions(val inAppStaking: List<NoPriceStakingDashboardOption<NoBalanceNoStake>>, val resolvingItems: Int)
}
