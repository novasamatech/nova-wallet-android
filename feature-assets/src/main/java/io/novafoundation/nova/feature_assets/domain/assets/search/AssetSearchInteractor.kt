package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.data.repository.AssetsViewModeService
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.assets.models.AssetFlowSearchResult
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.getAssetBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.getAssetGroupBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.getTokenAssetBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.getTokenAssetGroupBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByNetwork
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByToken
import io.novafoundation.nova.feature_assets.domain.common.searchTokens
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.aggregatedBalanceByAsset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainsById
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.enabledChainById
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private typealias AssetSearchFilter = suspend (Asset) -> Boolean

class AssetSearchInteractor(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val swapService: SwapService,
    private val assetsViewModeService: AssetsViewModeService
) {

    fun buyAssetSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetFlowSearchResult> {
        val filter = { asset: Asset -> asset.token.configuration.buyProviders.isNotEmpty() }

        return assetsViewModeService.assetsViewModeFlow().flatMapLatest { viewMode ->
            when (viewMode) {
                AssetViewMode.NETWORKS -> searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filter = filter)
                    .map { AssetFlowSearchResult.ByNetworks(it) }

                AssetViewMode.TOKENS -> searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, filter = filter)
                    .map { AssetFlowSearchResult.ByTokens(it) }
            }
        }
    }

    fun sendAssetSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetFlowSearchResult> {
        val filter = { asset: Asset -> asset.transferableInPlanks.isPositive() }

        return assetsViewModeService.assetsViewModeFlow().flatMapLatest { viewMode ->
            when (viewMode) {
                AssetViewMode.NETWORKS -> searchAssetsByNetworksInternalFlow(
                    queryFlow,
                    externalBalancesFlow,
                    assetGroupComparator = getAssetGroupBaseComparator { it.groupTransferableBalanceFiat },
                    assetsComparator = getAssetBaseComparator { it.balanceWithOffchain.transferable.fiat },
                    filter = filter
                ).map { AssetFlowSearchResult.ByNetworks(it) }

                AssetViewMode.TOKENS -> searchAssetsByTokensInternalFlow(
                    queryFlow,
                    externalBalancesFlow,
                    assetGroupComparator = getTokenAssetGroupBaseComparator { it.groupBalance.transferable.fiat },
                    assetsComparator = getTokenAssetBaseComparator { it.balanceWithOffChain.transferable.fiat },
                    filter = filter
                )
                    .map { AssetFlowSearchResult.ByTokens(it) }
            }
        }
    }

    fun searchSwapAssetsFlow(
        forAsset: FullChainAssetId?,
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        coroutineScope: CoroutineScope
    ): Flow<AssetFlowSearchResult> {
        val filterFlow = getAvailableSwapAssets(forAsset, coroutineScope).map { availableAssetsForSwap ->
            val filter: AssetSearchFilter = { asset ->
                val chainAsset = asset.token.configuration

                chainAsset.fullId in availableAssetsForSwap
            }

            filter
        }

        return assetsViewModeService.assetsViewModeFlow().flatMapLatest { viewMode ->
            when (viewMode) {
                AssetViewMode.NETWORKS -> searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filterFlow = filterFlow)
                    .map { AssetFlowSearchResult.ByNetworks(it) }

                AssetViewMode.TOKENS -> searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, filterFlow = filterFlow)
                    .map { AssetFlowSearchResult.ByTokens(it) }
            }
        }
    }

    fun searchReceiveAssetsFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetFlowSearchResult> {
        return assetsViewModeService.assetsViewModeFlow().flatMapLatest { viewMode ->
            when (viewMode) {
                AssetViewMode.NETWORKS -> searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filter = null)
                    .map { AssetFlowSearchResult.ByNetworks(it) }

                AssetViewMode.TOKENS -> searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, filter = null)
                    .map { AssetFlowSearchResult.ByTokens(it) }
            }
        }
    }

    fun searchAssetsFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>> {
        return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filter = null)
    }

    fun searchAssetsByNetworksInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<NetworkAssetGroup> = getAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithOffChainBalance> = getAssetBaseComparator(),
        filterFlow: Flow<AssetSearchFilter?>,
    ): Flow<Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>> {
        val assetsFlow = filteredAssetFlow(filterFlow)

        val aggregatedExternalBalances = externalBalancesFlow.map { it.aggregatedBalanceByAsset() }

        return combine(assetsFlow, aggregatedExternalBalances, queryFlow) { assets, externalBalances, query ->
            val chainsById = chainRegistry.enabledChainById()
            val filtered = assets.filterBy(query, chainsById)

            groupAndSortAssetsByNetwork(filtered, externalBalances, chainsById, assetGroupComparator, assetsComparator)
        }
    }

    fun searchAssetsByTokensInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<TokenAssetGroup> = getTokenAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithNetwork> = getTokenAssetBaseComparator(),
        filterFlow: Flow<AssetSearchFilter?>,
    ): Flow<Map<TokenAssetGroup, List<AssetWithNetwork>>> {
        val assetsFlow = filteredAssetFlow(filterFlow)

        val aggregatedExternalBalances = externalBalancesFlow.map { it.aggregatedBalanceByAsset() }

        return combine(assetsFlow, aggregatedExternalBalances, queryFlow) { assets, externalBalances, query ->
            val chainsById = chainRegistry.enabledChainById()
            val filtered = assets.filterBy(query, chainsById)

            groupAndSortAssetsByToken(filtered, externalBalances, chainsById, assetGroupComparator, assetsComparator)
        }
    }

    private fun filteredAssetFlow(filterFlow: Flow<AssetSearchFilter?>): Flow<List<Asset>> {
        val assetsFlow = accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { walletRepository.syncedAssetsFlow(it.id) }

        return combine(assetsFlow, filterFlow) { assets, filter ->
            if (filter == null) {
                assets
            } else {
                assets.filter { filter(it) }
            }
        }
    }

    private fun getAvailableSwapAssets(asset: FullChainAssetId?, coroutineScope: CoroutineScope): Flow<Set<FullChainAssetId>> {
        return flowOfAll {
            val chainAsset = asset?.let { chainRegistry.asset(it) }

            if (chainAsset == null) {
                swapService.assetsAvailableForSwap(coroutineScope)
            } else {
                swapService.availableSwapDirectionsFor(chainAsset, coroutineScope)
            }
        }
    }

    private fun List<Asset>.filterBy(query: String, chainsById: ChainsById): List<Asset> {
        return searchTokens(
            query = query,
            chainsById = chainsById,
            tokenSymbol = { it.token.configuration.symbol.value },
            relevantToChains = { asset, chainIds -> asset.token.configuration.chainId in chainIds }
        )
    }
}

private fun AssetSearchInteractor.searchAssetsByNetworksInternalFlow(
    queryFlow: Flow<String>,
    externalBalancesFlow: Flow<List<ExternalBalance>>,
    assetGroupComparator: Comparator<NetworkAssetGroup> = getAssetGroupBaseComparator(),
    assetsComparator: Comparator<AssetWithOffChainBalance> = getAssetBaseComparator(),
    filter: AssetSearchFilter?,
): Flow<Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>> {
    val filterFlow = flowOf(filter)

    return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, assetGroupComparator, assetsComparator, filterFlow)
}

private fun AssetSearchInteractor.searchAssetsByTokensInternalFlow(
    queryFlow: Flow<String>,
    externalBalancesFlow: Flow<List<ExternalBalance>>,
    assetGroupComparator: Comparator<TokenAssetGroup> = getTokenAssetGroupBaseComparator(),
    assetsComparator: Comparator<AssetWithNetwork> = getTokenAssetBaseComparator(),
    filter: AssetSearchFilter?,
): Flow<Map<TokenAssetGroup, List<AssetWithNetwork>>> {
    val filterFlow = flowOf(filter)

    return searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, assetGroupComparator, assetsComparator, filterFlow)
}
