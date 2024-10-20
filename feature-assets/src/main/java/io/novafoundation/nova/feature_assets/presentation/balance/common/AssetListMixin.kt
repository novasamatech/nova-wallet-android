package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.data.model.switch
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.common.utils.updateValue
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.list.AssetsListInteractor
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

class AssetListMixinFactory(
    private val walletInteractor: WalletInteractor,
    private val assetsListInteractor: AssetsListInteractor,
    private val currencyInteractor: CurrencyInteractor,
    private val externalBalancesInteractor: ExternalBalancesInteractor
) {

    fun create(coroutineScope: CoroutineScope): AssetListMixin = RealAssetListMixin(
        walletInteractor,
        assetsListInteractor,
        currencyInteractor,
        externalBalancesInteractor,
        coroutineScope
    )
}

interface AssetListMixin {

    val assetsFlow: Flow<List<Asset>>

    val filteredAssetsFlow: Flow<List<Asset>>

    val assetModelsFlow: Flow<List<BalanceListRvItem>>

    fun expandToken(tokenGroupUi: TokenGroupUi)
    suspend fun switchViewMode()
    val assetsViewModeFlow: Flow<AssetViewMode>
}

class RealAssetListMixin(
    private val walletInteractor: WalletInteractor,
    private val assetsListInteractor: AssetsListInteractor,
    private val currencyInteractor: CurrencyInteractor,
    private val externalBalancesInteractor: ExternalBalancesInteractor,
    private val coroutineScope: CoroutineScope
) : AssetListMixin, CoroutineScope by coroutineScope {

    override val assetsFlow = walletInteractor.assetsFlow()
        .shareInBackground()

    override val filteredAssetsFlow = walletInteractor.filterAssets(assetsFlow)
        .shareInBackground()

    private val selectedCurrency = currencyInteractor.observeSelectCurrency()
        .shareInBackground()

    private val externalBalancesFlow = externalBalancesInteractor.observeExternalBalances()
        .shareInBackground()

    private val expandedTokenIdsFlow = MutableStateFlow(setOf<String>())

    override val assetsViewModeFlow = assetsListInteractor.assetsViewModeFlow()
        .shareInBackground()

    override val assetModelsFlow = combine(
        filteredAssetsFlow,
        selectedCurrency,
        externalBalancesFlow,
        assetsViewModeFlow,
        expandedTokenIdsFlow
    ) { assets, currency, externalBalances, viewMode, expandedTokens ->
        when (viewMode) {
            AssetViewMode.NETWORKS -> walletInteractor.groupAssetsByNetwork(assets, externalBalances).mapGroupedAssetsToUi(currency)
            AssetViewMode.TOKENS -> walletInteractor.groupAssetsByToken(assets, externalBalances)
                .mapValues { filterExpandedItems(it, expandedTokens) }
                .mapGroupedAssetsToUi()
        }
    }.distinctUntilChanged()
        .shareInBackground()

    override suspend fun switchViewMode() {
        val assetViewMode = assetsViewModeFlow.first()

        assetsListInteractor.setAssetViewMode(assetViewMode.switch())
    }

    override fun expandToken(tokenGroupUi: TokenGroupUi) {
        expandedTokenIdsFlow.updateValue { it.toggle(tokenGroupUi.itemId) }
    }

    private fun filterExpandedItems(entry: Map.Entry<TokenAssetGroup, List<AssetWithNetwork>>, expandedTokens: Set<String>): List<AssetWithNetwork> {
        val group = entry.key
        val items = entry.value

        if (group.token.symbol.value in expandedTokens) {
            return items
        }

        return emptyList()
    }
}
