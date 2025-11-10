package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.common.utils.filterValueList
import io.novafoundation.nova.common.utils.scopeAsync
import io.novafoundation.nova.feature_assets.data.CanPayFeeAssetSharedComputation
import io.novafoundation.nova.feature_assets.domain.assets.models.AssetsByViewModeResult
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.getAssetBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.getAssetGroupBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByNetwork
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.aggregatedBalanceByAsset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.enabledChainById
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

class ByNetworkAssetSearchInteractor(
    private val assetSearchUseCase: AssetSearchUseCase,
    private val chainRegistry: ChainRegistry,
    private val tradeTokenRegistry: TradeTokenRegistry,
    private val canPayFeeAssetSharedComputation: CanPayFeeAssetSharedComputation
) : AssetSearchInteractor {

    override fun tradeAssetSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        tradeType: TradeTokenRegistry.TradeType
    ): Flow<AssetsByViewModeResult> {
        val filter = { asset: Asset -> tradeTokenRegistry.hasProvider(asset.token.configuration, tradeType) }

        return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filter = filter)
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
        val filterFlow = assetSearchUseCase.getAvailableSwapAssets(forAsset, coroutineScope).map { availableAssetsForSwap ->
            val filter: AssetSearchFilter = { asset ->
                val chainAsset = asset.token.configuration

                chainAsset.fullId in availableAssetsForSwap
            }

            filter
        }

        return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filterFlow = filterFlow)
    }

    override fun searchReceiveAssetsFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetsByViewModeResult> {
        return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filter = null)
    }

    override fun giftAssetsSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        coroutineScope: CoroutineScope
    ): Flow<AssetsByViewModeResult> {
        return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, filter = null)
            .transform { assetGroup ->
                val commissionAssets = assetGroup.assets
                    .filterValueList { it.asset.token.configuration.isCommissionAsset }
                emit(AssetsByViewModeResult.ByNetworks(commissionAssets))

                val allAvailableAssets = assetGroup.assets.mapValues { (_, assets) ->
                    canPayFeeAssetSharedComputation.assetsCanPayFeeFlow(assets, coroutineScope)
                }
                emit(AssetsByViewModeResult.ByNetworks(allAvailableAssets))
            }
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
    ): Flow<AssetsByViewModeResult.ByNetworks> {
        val filterFlow = flowOf(filter)

        return searchAssetsByNetworksInternalFlow(queryFlow, externalBalancesFlow, assetGroupComparator, assetsComparator, filterFlow)
    }

    private fun searchAssetsByNetworksInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<NetworkAssetGroup> = getAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithOffChainBalance> = getAssetBaseComparator(),
        filterFlow: Flow<AssetSearchFilter?>,
    ): Flow<AssetsByViewModeResult.ByNetworks> {
        val assetsFlow = assetSearchUseCase.filteredAssetFlow(filterFlow)

        val aggregatedExternalBalances = externalBalancesFlow.map { it.aggregatedBalanceByAsset() }

        return combine(assetsFlow, aggregatedExternalBalances, queryFlow) { assets, externalBalances, query ->
            val chainsById = chainRegistry.enabledChainById()
            val filtered = assetSearchUseCase.filterAssetsByQuery(query, assets, chainsById)

            val assetGroups = groupAndSortAssetsByNetwork(filtered, externalBalances, chainsById, assetGroupComparator, assetsComparator)
            AssetsByViewModeResult.ByNetworks(assetGroups)
        }
    }

    private suspend fun CanPayFeeAssetSharedComputation.assetsCanPayFeeFlow(
        assets: List<AssetWithOffChainBalance>,
        coroutineScope: CoroutineScope
    ): List<AssetWithOffChainBalance> {
        val assetsFlow = assets.map { asset ->
            scopeAsync {
                if (canPayFeeInAsset(asset.asset.token.configuration, coroutineScope)) {
                    asset
                } else {
                    null
                }
            }
        }

        return assetsFlow.awaitAll()
            .filterNotNull()
    }
}
