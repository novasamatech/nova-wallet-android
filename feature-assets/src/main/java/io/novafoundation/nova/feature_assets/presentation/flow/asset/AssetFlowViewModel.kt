package io.novafoundation.nova.feature_assets.presentation.flow.asset

import io.novafoundation.nova.common.base.BaseViewModel
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
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.mapTokenAssetGroupToUi
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
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
) : BaseViewModel() {

    protected val interactor = interactorFactory.createByAssetViewMode()

    val acknowledgeLedgerWarning = controllableAssetCheck.acknowledgeLedgerWarning

    val query = MutableStateFlow("")

    private val selectedCurrency = currencyInteractor.observeSelectCurrency()
        .inBackground()
        .share()

    protected val externalBalancesFlow = externalBalancesInteractor.observeExternalBalances()

    private val searchAssetsFlow = flowOfAll { searchAssetsFlow() }
        .shareInBackground()

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

    abstract fun assetClicked(assetModel: AssetModel)

    abstract fun tokenClicked(tokenGroup: TokenGroupUi)

    private fun mapAssets(searchResult: AssetsByViewModeResult, currency: Currency): List<BalanceListRvItem> {
        return when (searchResult) {
            is AssetsByViewModeResult.ByNetworks -> mapNetworkAssets(searchResult.assets, currency)
            is AssetsByViewModeResult.ByTokens -> mapTokensAssets(searchResult.tokens)
        }
    }

    open fun mapNetworkAssets(assets: Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>, currency: Currency): List<BalanceListRvItem> {
        return assets.mapGroupedAssetsToUi(currency)
    }

    open fun mapTokensAssets(assets: Map<TokenAssetGroup, List<AssetWithNetwork>>): List<BalanceListRvItem> {
        return assets.map { mapTokenAssetGroupToUi(it.key, assets = it.value) }
    }

    internal fun validate(assetModel: AssetModel, onAccept: (AssetModel) -> Unit) {
        launch {
            val metaAccount = accountUseCase.getSelectedMetaAccount()
            val chainAsset = assetModel.token.configuration
            controllableAssetCheck.check(metaAccount, chainAsset) {
                onAccept(assetModel)
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
