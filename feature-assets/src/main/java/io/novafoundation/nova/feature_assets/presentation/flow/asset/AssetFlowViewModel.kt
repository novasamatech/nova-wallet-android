package io.novafoundation.nova.feature_assets.presentation.flow.asset

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.model.AssetViewMode
import io.novafoundation.nova.common.domain.interactor.AssetViewModeInteractor
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.view.PlaceholderModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.models.AssetsByViewModeResult
import io.novafoundation.nova.feature_assets.domain.assets.models.groupList
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractorFactory
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.NetworkAssetFormatter
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.TokenAssetFormatter
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

abstract class AssetFlowViewModel(
    interactorFactory: AssetSearchInteractorFactory,
    protected val router: AssetsRouter,
    protected val currencyInteractor: CurrencyInteractor,
    private val controllableAssetCheck: ControllableAssetCheckMixin,
    protected val accountUseCase: SelectedAccountUseCase,
    externalBalancesInteractor: ExternalBalancesInteractor,
    protected val resourceManager: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    private val assetViewModeInteractor: AssetViewModeInteractor,
    private val networkAssetMapper: NetworkAssetFormatter,
    private val tokenAssetFormatter: TokenAssetFormatter
) : BaseViewModel() {

    protected val interactor = interactorFactory.createByAssetViewMode()

    val acknowledgeLedgerWarning = controllableAssetCheck.acknowledgeLedgerWarning

    val query = MutableStateFlow("")

    private val selectedCurrency = currencyInteractor.observeSelectCurrency()
        .inBackground()
        .share()

    protected val externalBalancesFlow = externalBalancesInteractor.observeExternalBalances()

    private val searchAssetsFlow = flowOfAll { searchAssetsFlow() } // lazy use searchAssetsFlow to let subclasses initialize self
        .shareInBackground(SharingStarted.Lazily)

    val searchHint = assetViewModeInteractor.assetsViewModeFlow()
        .map {
            when (it) {
                AssetViewMode.NETWORKS -> resourceManager.getString(R.string.assets_search_hint)
                AssetViewMode.TOKENS -> resourceManager.getString(R.string.assets_search_token_hint)
            }
        }

    val searchResults = combine(
        searchAssetsFlow, // lazy use searchAssetsFlow to let subclasses initialize self
        selectedCurrency,
    ) { assets, currency ->
        mapAssets(assets, currency)
    }.distinctUntilChanged()
        .shareInBackground(SharingStarted.Lazily)

    val placeholder = searchAssetsFlow.map { getPlaceholder(query.value, it.groupList()) }

    fun backClicked() {
        router.back()
    }

    abstract fun searchAssetsFlow(): Flow<AssetsByViewModeResult>

    abstract fun assetClicked(asset: Chain.Asset)

    abstract fun tokenClicked(tokenGroup: TokenGroupUi)

    private fun mapAssets(searchResult: AssetsByViewModeResult, currency: Currency): List<BalanceListRvItem> {
        return when (searchResult) {
            is AssetsByViewModeResult.ByNetworks -> mapNetworkAssets(searchResult.assets, currency)
            is AssetsByViewModeResult.ByTokens -> mapTokensAssets(searchResult.tokens)
        }
    }

    open fun mapNetworkAssets(assets: Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>, currency: Currency): List<BalanceListRvItem> {
        return networkAssetMapper.mapGroupedAssetsToUi(assets, assetIconProvider, currency)
    }

    open fun mapTokensAssets(assets: Map<TokenAssetGroup, List<AssetWithNetwork>>): List<BalanceListRvItem> {
        return assets.map { tokenAssetFormatter.mapTokenAssetGroupToUi(assetIconProvider, it.key, assets = it.value) }
    }

    internal fun validate(asset: Chain.Asset, onAccept: (Chain.Asset) -> Unit) {
        launch {
            val metaAccount = accountUseCase.getSelectedMetaAccount()
            controllableAssetCheck.check(metaAccount, asset) {
                onAccept(asset)
            }
        }
    }

    protected open fun getPlaceholder(query: String, assets: List<Any>): PlaceholderModel? {
        return when {
            assets.isEmpty() -> PlaceholderModel(
                text = resourceManager.getString(R.string.assets_search_placeholder),
                imageRes = R.drawable.ic_no_search_results
            )

            else -> null
        }
    }
}
