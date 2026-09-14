package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.feature_assets.domain.assets.models.AssetsByViewModeResult
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.getAssetBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.getAssetGroupBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByNetwork
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_gift_api.domain.AvailableGiftAssetsUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.aggregatedBalanceByAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.enabledChainById
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ByNetworkAssetSearchInteractor(
    private val assetSearchUseCase: AssetSearchUseCase,
    private val chainRegistry: ChainRegistry,
    private val tradeTokenRegistry: TradeTokenRegistry,
    private val availableGiftAssetsUseCase: AvailableGiftAssetsUseCase
) : AssetSearchInteractor {

    override fun tradeAssetSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        tradeType: TradeTokenRegistry.TradeType
    ): Flow<AssetsByViewModeResult> {
        val filter = { asset: Asset -> tradeTokenRegistry.hasProvider(asset.token.configuration, tradeType) }

        // Buying delivers a token you do not have yet, so the destination list is not filtered
        return searchAssetsByNetworksInternalFlow(
            queryFlow,
            externalBalancesFlow,
            filter = filter,
            onlyVisible = tradeType != TradeTokenRegistry.TradeType.BUY
        )
    }

    override fun sendAssetSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetsByViewModeResult> {
        val filter = { asset: Asset -> asset.transferableInPlanks.isPositive() }

        return searchAssetsByNetworksInternalFlow(
            queryFlow,
            externalBalancesFlow,
            assetGroupComparator = getAssetGroupBaseComparator { it.groupTransferableBalanceFiat },
            assetsComparator = getAssetBaseComparator { it.balanceWithOffchain.transferable.fiat },
            filter = filter
        )
    }

    override fun searchSwapAssetsFlow(
        forAsset: FullChainAssetId?,
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        coroutineScope: CoroutineScope
    ): Flow<AssetsByViewModeResult> {
        val filterFlow = assetSearchUseCase.getAvailableSwapAssets(forAsset, coroutineScope).mapToAssetSearchFilter()

        // A null source means this is the "swap from" picker; anything else is the destination
        return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filterFlow = filterFlow, onlyVisible = forAsset == null)
    }

    override fun searchReceiveAssetsFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetsByViewModeResult> {
        // Receiving is the whole point of picking a token you do not hold yet
        return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filter = null, onlyVisible = false)
    }

    override fun giftAssetsSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        coroutineScope: CoroutineScope
    ): Flow<AssetsByViewModeResult> {
        val filterFlow = availableGiftAssetsUseCase.getAvailableGiftAssets(coroutineScope).mapToAssetSearchFilter()
        return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filterFlow = filterFlow)
    }

    override fun searchAssetsFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetsByViewModeResult> {
        return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filter = null)
    }

    private fun ByNetworkAssetSearchInteractor.searchAssetsByNetworksInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<NetworkAssetGroup> = getAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithOffChainBalance> = getAssetBaseComparator(),
        filter: AssetSearchFilter?,
        onlyVisible: Boolean = true,
    ): Flow<AssetsByViewModeResult.ByNetworks> {
        val filterFlow = flowOf(filter)

        return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, assetGroupComparator, assetsComparator, filterFlow, onlyVisible)
    }

    private fun searchAssetsByNetworksInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<NetworkAssetGroup> = getAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithOffChainBalance> = getAssetBaseComparator(),
        filterFlow: Flow<AssetSearchFilter?>,
        onlyVisible: Boolean = true,
    ): Flow<AssetsByViewModeResult.ByNetworks> {
        val assetsFlow = assetSearchUseCase.filteredAssetFlow(filterFlow, onlyVisible)

        val aggregatedExternalBalances = externalBalancesFlow.map { it.aggregatedBalanceByAsset() }

        return combine(assetsFlow, aggregatedExternalBalances, queryFlow) { assets, externalBalances, query ->
            val chainsById = chainRegistry.enabledChainById()
            val filtered = assetSearchUseCase.filterAssetsByQuery(query, assets, chainsById)

            val assetGroups = groupAndSortAssetsByNetwork(filtered, externalBalances, chainsById, assetGroupComparator, assetsComparator)
            AssetsByViewModeResult.ByNetworks(assetGroups)
        }
    }
}
