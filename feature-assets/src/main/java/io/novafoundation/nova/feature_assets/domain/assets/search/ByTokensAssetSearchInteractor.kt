package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.feature_assets.domain.assets.models.AssetFlowSearchResult
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.getTokenAssetBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.getTokenAssetGroupBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByToken
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.aggregatedBalanceByAsset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.enabledChainById
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ByTokensAssetSearchInteractor(
    private val assetSearchUseCase: AssetSearchUseCase,
    private val chainRegistry: ChainRegistry
) : AssetSearchInteractor {

    override fun buyAssetSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetFlowSearchResult> {
        val filter = { asset: Asset -> asset.token.configuration.buyProviders.isNotEmpty() }

        return searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, filter = filter)
    }

    override fun sendAssetSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetFlowSearchResult> {
        val filter = { asset: Asset -> asset.transferableInPlanks.isPositive() }

        return searchAssetsByTokensInternalFlow(
            queryFlow,
            externalBalancesFlow,
            assetGroupComparator = getTokenAssetGroupBaseComparator { it.groupBalance.transferable.fiat },
            assetsComparator = getTokenAssetBaseComparator { it.balanceWithOffChain.transferable.fiat },
            filter = filter
        )
    }

    override fun searchSwapAssetsFlow(
        forAsset: FullChainAssetId?,
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        coroutineScope: CoroutineScope
    ): Flow<AssetFlowSearchResult> {
        val filterFlow = assetSearchUseCase.getAvailableSwapAssets(forAsset, coroutineScope).map { availableAssetsForSwap ->
            val filter: AssetSearchFilter = { asset ->
                val chainAsset = asset.token.configuration

                chainAsset.fullId in availableAssetsForSwap
            }

            filter
        }

        return searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, filterFlow = filterFlow)
    }

    override fun searchReceiveAssetsFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetFlowSearchResult> {
        return searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, filter = null)
    }

    override fun searchAssetsFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetFlowSearchResult> {
        return searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, filter = null)
    }

    private fun searchAssetsByTokensInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<TokenAssetGroup> = getTokenAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithNetwork> = getTokenAssetBaseComparator(),
        filter: AssetSearchFilter?,
    ): Flow<AssetFlowSearchResult> {
        val filterFlow = flowOf(filter)

        return searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, assetGroupComparator, assetsComparator, filterFlow)
    }

    private fun searchAssetsByTokensInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<TokenAssetGroup> = getTokenAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithNetwork> = getTokenAssetBaseComparator(),
        filterFlow: Flow<AssetSearchFilter?>,
    ): Flow<AssetFlowSearchResult> {
        val assetsFlow = assetSearchUseCase.filteredAssetFlow(filterFlow)

        val aggregatedExternalBalances = externalBalancesFlow.map { it.aggregatedBalanceByAsset() }

        return combine(assetsFlow, aggregatedExternalBalances, queryFlow) { assets, externalBalances, query ->
            val chainsById = chainRegistry.enabledChainById()
            val filtered = assetSearchUseCase.filterAssetsByQuery(query, assets, chainsById)

            val assetGroups = groupAndSortAssetsByToken(filtered, externalBalances, chainsById, assetGroupComparator, assetsComparator)

            AssetFlowSearchResult.ByTokens(assetGroups)
        }
    }
}
