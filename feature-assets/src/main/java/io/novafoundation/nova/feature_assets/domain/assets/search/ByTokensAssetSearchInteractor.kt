package io.novafoundation.nova.feature_assets.domain.assets.search

import io.novafoundation.nova.feature_assets.domain.assets.models.AssetsByViewModeResult
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.getTokenAssetBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.getTokenAssetGroupBaseComparator
import io.novafoundation.nova.feature_assets.domain.common.groupAndSortAssetsByToken
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

class ByTokensAssetSearchInteractor(
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
        return searchAssetsByTokensInternalFlow(
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
    ): Flow<AssetsByViewModeResult> {
        val filterFlow = assetSearchUseCase.getAvailableSwapAssets(forAsset, coroutineScope).mapToAssetSearchFilter()

        // A null source means this is the "swap from" picker; anything else is the destination
        return searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, filterFlow = filterFlow, onlyVisible = forAsset == null)
    }

    override fun searchReceiveAssetsFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetsByViewModeResult> {
        // Receiving is the whole point of picking a token you do not hold yet
        return searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, filter = null, onlyVisible = false)
    }

    override fun giftAssetsSearch(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        coroutineScope: CoroutineScope
    ): Flow<AssetsByViewModeResult> {
        val filterFlow = availableGiftAssetsUseCase.getAvailableGiftAssets(coroutineScope).mapToAssetSearchFilter()
        return searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, filterFlow = filterFlow)
    }

    override fun searchAssetsFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
    ): Flow<AssetsByViewModeResult.ByTokens> {
        return searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, filter = null)
    }

    private fun searchAssetsByTokensInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<TokenAssetGroup> = getTokenAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithNetwork> = getTokenAssetBaseComparator(),
        filter: AssetSearchFilter?,
        onlyVisible: Boolean = true,
    ): Flow<AssetsByViewModeResult.ByTokens> {
        val filterFlow = flowOf(filter)

        return searchAssetsByTokensInternalFlow(queryFlow, externalBalancesFlow, assetGroupComparator, assetsComparator, filterFlow, onlyVisible)
    }

    private fun searchAssetsByTokensInternalFlow(
        queryFlow: Flow<String>,
        externalBalancesFlow: Flow<List<ExternalBalance>>,
        assetGroupComparator: Comparator<TokenAssetGroup> = getTokenAssetGroupBaseComparator(),
        assetsComparator: Comparator<AssetWithNetwork> = getTokenAssetBaseComparator(),
        filterFlow: Flow<AssetSearchFilter?>,
        onlyVisible: Boolean = true,
    ): Flow<AssetsByViewModeResult.ByTokens> {
        val assetsFlow = assetSearchUseCase.filteredAssetFlow(filterFlow, onlyVisible)

        val aggregatedExternalBalances = externalBalancesFlow.map { it.aggregatedBalanceByAsset() }

        return combine(assetsFlow, aggregatedExternalBalances, queryFlow) { assets, externalBalances, query ->
            val chainsById = chainRegistry.enabledChainById()
            val filtered = assetSearchUseCase.filterAssetsByQuery(query, assets, chainsById)

            val assetGroups = groupAndSortAssetsByToken(filtered, externalBalances, chainsById, assetGroupComparator, assetsComparator)

            AssetsByViewModeResult.ByTokens(assetGroups)
        }
    }
}
