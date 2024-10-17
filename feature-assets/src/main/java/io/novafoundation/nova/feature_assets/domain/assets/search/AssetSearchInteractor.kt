package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.getAssetBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.getAssetGroupBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByNetwork
import io.novafoundation.nova.feature_assets.domain.common.searchTokens
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
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
    private val assetSourceRegistry: AssetSourceRegistry,
    private val swapService: SwapService
) {

    fun buyAssetSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>> {
        return searchAssetsInternalFlow(queryFlow, externalBalancesFlow) {
            it.token.configuration.buyProviders.isNotEmpty()
        }
    }

    fun sendAssetSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>> {
        val groupComparator = getAssetGroupBaseComparator { it.groupTransferableBalanceFiat }
        val assetsComparator = getAssetBaseComparator { it.balanceWithOffchain.transferable.fiat }

        return searchAssetsInternalFlow(queryFlow, externalBalancesFlow, groupComparator, assetsComparator) { asset ->
            asset.transferableInPlanks.isPositive()
        }
    }

    fun searchSwapAssetsFlow(
        forAsset: FullChainAssetId?,
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        coroutineScope: CoroutineScope
    ): Flow<Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>> {
        val filterFlow = getAvailableSwapAssets(forAsset, coroutineScope).map { availableAssetsForSwap ->
            val filter: AssetSearchFilter = { asset ->
                val chainAsset = asset.token.configuration

                chainAsset.fullId in availableAssetsForSwap
            }

            filter
        }

        return searchAssetsInternalFlow(queryFlow, externalBalancesFlow, filterFlow = filterFlow)
    }

    fun searchAssetsFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>> {
        return searchAssetsInternalFlow(queryFlow, externalBalancesFlow, filter = null)
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

    private fun searchAssetsInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<NetworkAssetGroup> = getAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithOffChainBalance> = getAssetBaseComparator(),
        filter: AssetSearchFilter?,
    ): Flow<Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>> {
        val filterFlow = flowOf(filter)

        return searchAssetsInternalFlow(queryFlow, externalBalancesFlow, assetGroupComparator, assetsComparator, filterFlow)
    }

    private fun searchAssetsInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<NetworkAssetGroup> = getAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithOffChainBalance> = getAssetBaseComparator(),
        filterFlow: Flow<AssetSearchFilter?>,
    ): Flow<Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>> {
        var assetsFlow = accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { walletRepository.syncedAssetsFlow(it.id) }

        assetsFlow = combine(assetsFlow, filterFlow) { assets, filter ->
            if (filter == null) {
                assets
            } else {
                assets.filter { filter(it) }
            }
        }

        val aggregatedExternalBalances = externalBalancesFlow.map { it.aggregatedBalanceByAsset() }

        return combine(assetsFlow, aggregatedExternalBalances, queryFlow) { assets, externalBalances, query ->
            val chainsById = chainRegistry.enabledChainById()
            val filtered = assets.filterBy(query, chainsById)

            groupAndSortAssetsByNetwork(filtered, externalBalances, chainsById, assetGroupComparator, assetsComparator)
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
