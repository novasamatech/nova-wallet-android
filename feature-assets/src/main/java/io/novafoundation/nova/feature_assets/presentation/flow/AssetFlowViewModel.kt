package io.novafoundation.nova.feature_assets.presentation.flow

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.view.PlaceholderModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AssetFlowListModel(
    val assets: List<Any>,
    val placeholder: PlaceholderModel?,
)

abstract class AssetFlowViewModel(
    protected val interactor: AssetSearchInteractor,
    protected val router: AssetsRouter,
    protected val currencyInteractor: CurrencyInteractor,
    private val controllableAssetCheck: ControllableAssetCheckMixin,
    internal val accountUseCase: SelectedAccountUseCase,
    externalBalancesInteractor: ExternalBalancesInteractor,
    internal val resourceManager: ResourceManager,
) : BaseViewModel() {

    val acknowledgeLedgerWarning = controllableAssetCheck.acknowledgeLedgerWarning

    val query = MutableStateFlow("")

    private val selectedCurrency = currencyInteractor.observeSelectCurrency()
        .inBackground()
        .share()

    protected val externalBalancesFlow = externalBalancesInteractor.observeExternalBalances()

    val searchResults = combine(
        searchAssetsFlow(),
        selectedCurrency,
    ) { assets, currency ->
        val groupedAssets = assets.mapGroupedAssetsToUi(currency)
        AssetFlowListModel(
            groupedAssets,
            getPlaceholder(query.value, groupedAssets)
        )
    }
        .distinctUntilChanged()
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    abstract fun searchAssetsFlow(): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>>

    abstract fun assetClicked(assetModel: AssetModel)

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
