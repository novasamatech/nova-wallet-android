package io.novafoundation.nova.feature_assets.domain.networks

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.filterList
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.getTokenAssetBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.getTokenAssetGroupBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByToken
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.aggregatedBalanceByAsset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
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

private typealias AssetFilter = suspend (Asset) -> Boolean

class AssetNetworksInteractor(
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val swapService: SwapService
) {

    fun buyAssetFlow(
        tokenSymbol: TokenSymbol,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<List<AssetWithNetwork>> {
        val filter = { asset: Asset -> asset.token.configuration.buyProviders.isNotEmpty() }

        return searchAssetsByTokenSymbolInternalFlow(tokenSymbol, externalBalancesFlow, filter = filter)
    }

    fun sendAssetFlow(
        tokenSymbol: TokenSymbol,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<List<AssetWithNetwork>> {
        val filter = { asset: Asset -> asset.transferableInPlanks.isPositive() }

        return searchAssetsByTokenSymbolInternalFlow(
            tokenSymbol,
            externalBalancesFlow,
            assetGroupComparator = getTokenAssetGroupBaseComparator { it.groupBalance.transferable.fiat },
            assetsComparator = getTokenAssetBaseComparator { it.balanceWithOffChain.transferable.fiat },
            filter = filter
        )
    }

    fun swapAssetsFlow(
        forAsset: FullChainAssetId?,
        tokenSymbol: TokenSymbol,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        coroutineScope: CoroutineScope
    ): Flow<List<AssetWithNetwork>> {
        val filterFlow = getAvailableSwapAssets(forAsset, coroutineScope).map { availableAssetsForSwap ->
            val filter: AssetFilter = { asset ->
                val chainAsset = asset.token.configuration

                chainAsset.fullId in availableAssetsForSwap
            }

            filter
        }

        return searchAssetsByTokenSymbolInternalFlow(tokenSymbol, externalBalancesFlow, filterFlow = filterFlow)
    }

    fun receiveAssetFlow(
        tokenSymbol: TokenSymbol,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<List<AssetWithNetwork>> {
        return searchAssetsByTokenSymbolInternalFlow(tokenSymbol, externalBalancesFlow, filter = null)
    }

    fun searchAssetsByTokenSymbolInternalFlow(
        tokenSymbol: TokenSymbol,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<TokenAssetGroup> = getTokenAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithNetwork> = getTokenAssetBaseComparator(),
        filterFlow: Flow<AssetFilter?>,
    ): Flow<List<AssetWithNetwork>> {
        val assetsFlow = filteredAssetFlow(filterFlow)
            .filterList { it.token.configuration.symbol == tokenSymbol }

        val aggregatedExternalBalances = externalBalancesFlow.map { it.aggregatedBalanceByAsset() }

        return combine(assetsFlow, aggregatedExternalBalances) { assets, externalBalances ->
            val chainsById = chainRegistry.enabledChainById()

            groupAndSortAssetsByToken(assets, externalBalances, chainsById, assetGroupComparator, assetsComparator)
                .flatMap { it.value }
        }
    }

    private fun filteredAssetFlow(filterFlow: Flow<AssetFilter?>): Flow<List<Asset>> {
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
}

private fun AssetNetworksInteractor.searchAssetsByTokenSymbolInternalFlow(
    tokenSymbol: TokenSymbol,
    externalBalancesFlow: Flow<List<ExternalBalance>>,
    assetGroupComparator: Comparator<TokenAssetGroup> = getTokenAssetGroupBaseComparator(),
    assetsComparator: Comparator<AssetWithNetwork> = getTokenAssetBaseComparator(),
    filter: AssetFilter?,
): Flow<List<AssetWithNetwork>> {
    val filterFlow = flowOf(filter)

    return searchAssetsByTokenSymbolInternalFlow(tokenSymbol, externalBalancesFlow, assetGroupComparator, assetsComparator, filterFlow)
}
