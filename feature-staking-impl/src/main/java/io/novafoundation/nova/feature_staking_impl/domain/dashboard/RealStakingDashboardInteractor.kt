@file:OptIn(ExperimentalCoroutinesApi::class)

package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.asLoaded
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.fromOption
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_api.data.model.isStaking
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardSyncTracker
import io.novafoundation.nova.feature_staking_api.data.dashboard.SyncingStageMap
import io.novafoundation.nova.feature_staking_api.data.dashboard.getSyncingStage
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NotYetResolved
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.SyncingStage
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.WithoutStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.MoreStakingOptions
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDApp
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_api.data.dashboard.common.stakingChainsById
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardItem
import io.novafoundation.nova.feature_staking_impl.data.dashboard.repository.StakingDashboardRepository
import io.novafoundation.nova.feature_staking_impl.data.dashboard.repository.TotalStakeChainComparatorProvider
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.alphabeticalOrder
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.ext.relaychainsFirstAscendingOrder
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.ext.testnetsLastAscendingOrder
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
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
    private val totalStakeChainComparatorProvider: TotalStakeChainComparatorProvider,
) : StakingDashboardInteractor {

    override suspend fun syncDapps() {
        runCatching {
            withContext(Dispatchers.Default) {
                dAppMetadataRepository.syncDAppMetadatas()
            }
        }
    }

    override fun stakingDashboardFlow(): Flow<ExtendedLoadingState<StakingDashboard>> {
        return flow {
            val stakingChains = chainRegistry.stakingChainsById()
            val knownStakingAssets = stakingChains.knownStakingAssets()

            emit(ExtendedLoadingState.Loading)

            val dashboardFlow = accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
                val noPriceDashboardFlow = combine(
                    dashboardRepository.dashboardItemsFlow(metaAccount.id),
                    stakingDashboardSyncTracker.syncedItemsFlow,
                ) { dashboardItems, syncedItems ->
                    constructStakingDashboard(stakingChains, dashboardItems, syncedItems)
                }

                val assetsFlow = walletRepository.supportedAssetsByIdFlow(metaAccount.id, knownStakingAssets)

                combine(noPriceDashboardFlow, assetsFlow, ::addPricesToDashboard)
            }

            emitAll(dashboardFlow)
        }
    }

    override fun moreStakingOptionsFlow(): Flow<MoreStakingOptions> {
        return flow {
            val stakingChains = chainRegistry.stakingChainsById()
            val knownStakingAssets = stakingChains.knownStakingAssets()
            val metaAccount = accountRepository.getSelectedMetaAccount()

            val noPriceDashboardFlow = combine(
                dashboardRepository.dashboardItemsFlow(metaAccount.id),
                stakingDashboardSyncTracker.syncedItemsFlow,
            ) { dashboardItems, syncedItems ->
                constructMoreStakingOptions(stakingChains, dashboardItems, syncedItems)
            }

            val assetsFlow = walletRepository.supportedAssetsByIdFlow(metaAccount.id, knownStakingAssets)
            val dApps = dAppMetadataRepository.stakingDAppsFlow()
                .mapLoading { dapps -> dapps.sortedBy { it.name } }

            val dashboardFlow = combine(
                noPriceDashboardFlow,
                assetsFlow,
                dApps,
                ::combineNoMoreOptionsInfo
            )

            emitAll(dashboardFlow)
        }
    }

    private fun WalletRepository.supportedAssetsByIdFlow(metaId: Long, chainAssets: List<Chain.Asset>): Flow<Map<FullChainAssetId, Asset>> {
        return supportedAssetsFlow(metaId, chainAssets)
            .map { assets -> assets.associateBy { asset -> asset.token.configuration.fullId } }
    }

    private fun constructStakingDashboard(
        stakingChains: ChainsById,
        dashboardItems: List<StakingDashboardItem>,
        syncingStageMap: SyncingStageMap
    ): NoPriceStakingDashboard {
        val itemsByChainAndAsset = dashboardItems
            .groupBy { it.fullChainAssetId.chainId }
            .mapValues { (_, chainAssets) -> chainAssets.groupBy { it.fullChainAssetId.assetId } }

        val hasStake = mutableListOf<NoPriceStakingDashboardOption<HasStake>>()
        val noStake = mutableListOf<NoPriceStakingDashboardOption<NoBalanceNoStake>>()
        val notYetResolved = mutableListOf<NoPriceStakingDashboardOption<NotYetResolved>>()

        stakingChains.values.forEach { chain ->
            val itemsByChain = itemsByChainAndAsset[chain.id]

            if (itemsByChain == null) {
                if (!chain.isTestNet) {
                    notYetResolved.add(notYetResolvedChainOption(chain, chain.utilityAsset))
                }
                return@forEach
            }

            itemsByChain.forEach innerForEach@{ assetId, dashboardItems ->
                val asset = chain.assetsById[assetId] ?: return@innerForEach

                if (dashboardItems.isNoStakePresent()) {
                    if (!chain.isTestNet) {
                        noStake.add(noStakeAggregatedOption(chain, asset, dashboardItems, syncingStageMap))
                    }
                } else {
                    val hasStakingOptionsSize = dashboardItems.count { it.stakeState is StakingDashboardItem.StakeState.HasStake }
                    val shouldShowStakingType = hasStakingOptionsSize > 1

                    val hasStakeOptions = dashboardItems.mapNotNull { item -> hasStakeOption(chain, asset, shouldShowStakingType, item, syncingStageMap) }
                    hasStake.addAll(hasStakeOptions)
                }
            }
        }

        return NoPriceStakingDashboard(
            hasStake = hasStake,
            noStake = noStake,
            notYetResolved = notYetResolved
        )
    }

    private fun constructMoreStakingOptions(
        stakingChains: ChainsById,
        dashboardItems: List<StakingDashboardItem>,
        syncingStageMap: SyncingStageMap,
    ): NoPriceMoreStakingOptions {
        val itemsByChainAndAsset = dashboardItems
            .groupBy { it.fullChainAssetId.chainId }
            .mapValues { (_, chainAssets) -> chainAssets.groupBy { it.fullChainAssetId.assetId } }

        val noStake = mutableListOf<NoPriceStakingDashboardOption<NoBalanceNoStake>>()
        val notYetResolved = mutableListOf<NoPriceStakingDashboardOption<NotYetResolved>>()

        stakingChains.values.forEach { chain ->
            val itemsByChain = itemsByChainAndAsset[chain.id]

            if (itemsByChain == null) {
                if (chain.isTestNet) {
                    notYetResolved.add(notYetResolvedChainOption(chain, chain.utilityAsset))
                }
                return@forEach
            }

            itemsByChain.forEach innerForEach@{ assetId, dashboardItems ->
                val asset = chain.assetsById[assetId] ?: return@innerForEach

                if (dashboardItems.isNoStakePresent()) {
                    if (chain.isTestNet) {
                        noStake.add(noStakeAggregatedOption(chain, asset, dashboardItems, syncingStageMap))
                    }
                } else {
                    val separateNoStakeOptions = dashboardItems.filter { it.stakeState is StakingDashboardItem.StakeState.NoStake }
                        .map { noStakeSeparateOption(chain, asset, it, syncingStageMap) }

                    noStake.addAll(separateNoStakeOptions)
                }
            }
        }

        return NoPriceMoreStakingOptions(noStake, notYetResolved)
    }

    private suspend fun addPricesToDashboard(
        noPriceStakingDashboard: NoPriceStakingDashboard,
        assets: Map<FullChainAssetId, Asset>,
    ): ExtendedLoadingState<StakingDashboard> {
        val hasStakeOptions = noPriceStakingDashboard.hasStake.map { addPriceToHasStakeItem(it, assets) }
        val noStakeOptions = noPriceStakingDashboard.noStake.map { addAssetInfoToNoStakeItem(it, assets) }
        val notYetResolvedOptions = noPriceStakingDashboard.notYetResolved.map { addAssetInfoToNotYetResolvedItem(it, assets) }

        return StakingDashboard(
            hasStake = hasStakeOptions.sortedByChain(),
            withoutStake = (noStakeOptions + notYetResolvedOptions).sortedByChain(),
        ).asLoaded()
    }

    private suspend fun combineNoMoreOptionsInfo(
        noPriceMoreStakingOptions: NoPriceMoreStakingOptions,
        assets: Map<FullChainAssetId, Asset>,
        stakingDApps: ExtendedLoadingState<List<StakingDApp>>,
    ): MoreStakingOptions {
        val noStakeOptions = noPriceMoreStakingOptions.noStake.map { addAssetInfoToNoStakeItem(it, assets) }
        val notYetResolvedOptions = noPriceMoreStakingOptions.notYetResolved.map { addAssetInfoToNotYetResolvedItem(it, assets) }

        return MoreStakingOptions(
            inAppStaking = (noStakeOptions + notYetResolvedOptions).sortedByChain(),
            browserStaking = stakingDApps
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

    private fun addAssetInfoToNotYetResolvedItem(
        item: NoPriceStakingDashboardOption<NotYetResolved>,
        assets: Map<FullChainAssetId, Asset>,
    ): AggregatedStakingDashboardOption<NotYetResolved> {
        val asset = assets.getValue(item.chainAsset.fullId)

        return AggregatedStakingDashboardOption(
            chain = item.chain,
            token = asset.token,
            stakingState = item.stakingState,
            syncingStage = item.syncingStage
        )
    }

    private fun List<StakingDashboardItem>.isNoStakePresent() = all { it.stakeState is StakingDashboardItem.StakeState.NoStake }

    private fun List<StakingDashboardItem>.findMaxEarnings(): Percent? = mapNotNull {
        it.stakeState.stats.dataOrNull?.estimatedEarnings
    }.maxOrNull()

    private fun notYetResolvedChainOption(
        chain: Chain,
        chainAsset: Chain.Asset,
    ): NoPriceStakingDashboardOption<NotYetResolved> {
        return NoPriceStakingDashboardOption(
            chain = chain,
            chainAsset = chainAsset,
            stakingState = NotYetResolved,
            syncingStage = SyncingStage.SYNCING_ALL
        )
    }

    private fun noStakeAggregatedOption(
        chain: Chain,
        chainAsset: Chain.Asset,
        noStakeItems: List<StakingDashboardItem>,
        syncingStageMap: SyncingStageMap,
    ): NoPriceStakingDashboardOption<NoBalanceNoStake> {
        val maxEarnings = noStakeItems.findMaxEarnings()
        val stats = maxEarnings?.let(NoStake::Stats)

        val flowType = if (noStakeItems.size > 1) {
            NoStake.FlowType.Aggregated(noStakeItems.map { it.stakingType })
        } else {
            // aggregating means there is no staking present, hence we always hide staking type badge
            NoStake.FlowType.Single(noStakeItems.single().stakingType, showStakingType = false)
        }

        return NoPriceStakingDashboardOption(
            chain = chain,
            chainAsset = chainAsset,
            stakingState = NoBalanceNoStake(
                stats = ExtendedLoadingState.fromOption(stats),
                flowType = flowType
            ),
            syncingStage = chainAsset.supportedStakingOptions().minOf { stakingType ->
                val stakingOptionId = StakingOptionId(chain.id, chainAsset.id, stakingType)
                syncingStageMap.getSyncingStage(stakingOptionId)
            }
        )
    }

    private fun noStakeSeparateOption(
        chain: Chain,
        chainAsset: Chain.Asset,
        noStakeItem: StakingDashboardItem,
        syncingStageMap: SyncingStageMap,
    ): NoPriceStakingDashboardOption<NoBalanceNoStake> {
        val stats = noStakeItem.stakeState.stats.map {
            NoStake.Stats(it.estimatedEarnings)
        }

        return NoPriceStakingDashboardOption(
            chain = chain,
            chainAsset = chainAsset,
            stakingState = NoBalanceNoStake(
                stats = stats,
                flowType = NoStake.FlowType.Single(noStakeItem.stakingType, showStakingType = true)
            ),
            syncingStage = syncingStageMap.getSyncingStage(StakingOptionId(chain.id, chainAsset.id, noStakeItem.stakingType))
        )
    }

    private fun hasStakeOption(
        chain: Chain,
        chainAsset: Chain.Asset,
        showStakingType: Boolean,
        item: StakingDashboardItem,
        syncingStageMap: SyncingStageMap,
    ): NoPriceStakingDashboardOption<HasStake>? {
        if (item.stakeState !is StakingDashboardItem.StakeState.HasStake) return null

        return NoPriceStakingDashboardOption(
            chain = chain,
            chainAsset = chainAsset,
            stakingState = HasStake(
                showStakingType = showStakingType,
                stats = item.stakeState.stats.map(::mapItemStatsToOptionStats),
                stakingType = item.stakingType,
                stake = item.stakeState.stake
            ),
            syncingStage = syncingStageMap.getSyncingStage(StakingOptionId(chain.id, chainAsset.id, item.stakingType))
        )
    }

    private suspend fun List<AggregatedStakingDashboardOption<WithoutStake>>.sortedByChain(): List<AggregatedStakingDashboardOption<WithoutStake>> {
        val chainTotalStakeComparator = totalStakeChainComparatorProvider.getTotalStakeComparator()

        return sortedWith(
            compareBy<AggregatedStakingDashboardOption<WithoutStake>> { it.chain.relaychainsFirstAscendingOrder }
                .thenByDescending { it.token.planksToFiat(it.stakingState.availableBalance()) }
                .thenByDescending { it.stakingState.availableBalance() }
                .thenComparing(Comparator.comparing(AggregatedStakingDashboardOption<*>::chain, chainTotalStakeComparator))
                .thenBy { it.chain.alphabeticalOrder }
        )
    }

    @JvmName("sortedHasStakeByChain")
    private fun List<AggregatedStakingDashboardOption<HasStake>>.sortedByChain(): List<AggregatedStakingDashboardOption<HasStake>> {
        return sortedWith(
            compareByDescending<AggregatedStakingDashboardOption<HasStake>> { it.token.planksToFiat(it.stakingState.stake) }
                .thenByDescending { it.stakingState.stake }
                .thenBy { it.chain.testnetsLastAscendingOrder }
                .thenBy { it.chain.alphabeticalOrder }
        )
    }

    private fun WithoutStake.availableBalance(): Balance {
        return when (this) {
            is NoStake -> availableBalance
            NotYetResolved -> Balance.ZERO
        }
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
            StakingTypeGroup.NOMINATION_POOL -> transferableInPlanks
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
        val notYetResolved: List<NoPriceStakingDashboardOption<NotYetResolved>>,
    )

    private class NoBalanceNoStake(
        val stats: ExtendedLoadingState<NoStake.Stats>,
        val flowType: NoStake.FlowType
    )

    private class NoPriceMoreStakingOptions(
        val noStake: List<NoPriceStakingDashboardOption<NoBalanceNoStake>>,
        val notYetResolved: List<NoPriceStakingDashboardOption<NotYetResolved>>
    )
}
