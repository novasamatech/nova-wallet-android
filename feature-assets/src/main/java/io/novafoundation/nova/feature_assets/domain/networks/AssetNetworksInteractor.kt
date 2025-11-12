package io.novafoundation.nova.feature_assets.domain.networks

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.filterList
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchFilter
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchUseCase
import io.novafoundation.nova.feature_assets.domain.assets.search.mapToAssetSearchFilter
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.getTokenAssetBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.getTokenAssetGroupBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByToken
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.aggregatedBalanceByAsset
import io.novafoundation.nova.runtime.ext.normalize
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.enabledChainById
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class AssetNetworksInteractor(
    private val chainRegistry: ChainRegistry,
    private val assetSearchUseCase: AssetSearchUseCase,
    private val tradeTokenRegistry: TradeTokenRegistry,
    private val assetSourceRegistry: AssetSourceRegistry
) {

    fun tradeAssetFlow(
        tokenSymbol: TokenSymbol,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        tradeType: TradeTokenRegistry.TradeType
    ): Flow<List<AssetWithNetwork>> {
        val filter = { asset: Asset -> tradeTokenRegistry.hasProvider(asset.token.configuration, tradeType) }

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
        forAssetId: FullChainAssetId?,
        tokenSymbol: TokenSymbol,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        coroutineScope: CoroutineScope
    ): Flow<List<AssetWithNetwork>> {
        val filterFlow = assetSearchUseCase.getAvailableSwapAssets(forAssetId, coroutineScope).mapToAssetSearchFilter()
        return searchAssetsByTokenSymbolInternalFlow(tokenSymbol, externalBalancesFlow, filterFlow = filterFlow)
    }

    fun receiveAssetFlow(
        tokenSymbol: TokenSymbol,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<List<AssetWithNetwork>> {
        return searchAssetsByTokenSymbolInternalFlow(tokenSymbol, externalBalancesFlow, filter = null)
    }

    fun giftsAssetFlow(
        tokenSymbol: TokenSymbol,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        coroutineScope: CoroutineScope
    ): Flow<List<AssetWithNetwork>> {
        val filterFlow = assetSearchUseCase.getAvailableGiftAssets(coroutineScope).mapToAssetSearchFilter()
        return searchAssetsByTokenSymbolInternalFlow(
            tokenSymbol,
            externalBalancesFlow,
            assetGroupComparator = getTokenAssetGroupBaseComparator { it.groupBalance.transferable.fiat },
            assetsComparator = getTokenAssetBaseComparator { it.balanceWithOffChain.transferable.fiat },
            filterFlow = filterFlow
        )
    }

    fun searchAssetsByTokenSymbolInternalFlow(
        tokenSymbol: TokenSymbol,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<TokenAssetGroup> = getTokenAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithNetwork> = getTokenAssetBaseComparator(),
        filterFlow: Flow<AssetSearchFilter?>,
    ): Flow<List<AssetWithNetwork>> {
        val assetsFlow = assetSearchUseCase.filteredAssetFlow(filterFlow)
            .filterList { it.token.configuration.symbol.normalize() == tokenSymbol }

        val aggregatedExternalBalances = externalBalancesFlow.map { it.aggregatedBalanceByAsset() }

        return combine(assetsFlow, aggregatedExternalBalances) { assets, externalBalances ->
            val chainsById = chainRegistry.enabledChainById()

            groupAndSortAssetsByToken(assets, externalBalances, chainsById, assetGroupComparator, assetsComparator)
                .flatMap { it.value }
        }
    }

    private fun getSwapAssetsFilter(sourceAsset: FullChainAssetId?, coroutineScope: CoroutineScope): Flow<AssetSearchFilter> {
        return assetSearchUseCase.getAvailableSwapAssets(sourceAsset, coroutineScope).mapToAssetSearchFilter()
    }
}

private fun AssetNetworksInteractor.searchAssetsByTokenSymbolInternalFlow(
    tokenSymbol: TokenSymbol,
    externalBalancesFlow: Flow<List<ExternalBalance>>,
    assetGroupComparator: Comparator<TokenAssetGroup> = getTokenAssetGroupBaseComparator(),
    assetsComparator: Comparator<AssetWithNetwork> = getTokenAssetBaseComparator(),
    filter: AssetSearchFilter?,
): Flow<List<AssetWithNetwork>> {
    val filterFlow = flowOf(filter)

    return searchAssetsByTokenSymbolInternalFlow(tokenSymbol, externalBalancesFlow, assetGroupComparator, assetsComparator, filterFlow)
}
