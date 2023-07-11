package io.novafoundation.nova.feature_assets.presentation.flow

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

abstract class AssetFlowViewModel(
    internal val router: AssetsRouter,
    internal val interactor: AssetSearchInteractor,
    internal val currencyInteractor: CurrencyInteractor,
    internal val contributionsInteractor: ContributionsInteractor,
    internal val controllableAssetCheck: ControllableAssetCheckMixin,
    internal val accountUseCase: SelectedAccountUseCase,
) : BaseViewModel() {

    val acknowledgeLedgerWarning = controllableAssetCheck.acknowledgeLedgerWarning

    val query = MutableStateFlow("")

    internal val selectedCurrency = currencyInteractor.observeSelectCurrency()
        .inBackground()
        .share()

    internal val totalContributedByAssetsFlow = contributionsInteractor.observeTotalContributedByAssets()

    val searchResults = combine(
        searchAssetsFlow(),
        selectedCurrency,
    ) { assets, currency ->
        assets.mapGroupedAssetsToUi(currency)
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
}
