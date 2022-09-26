package io.novafoundation.nova.feature_assets.presentation.balance.search

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class AssetSearchViewModel(
    private val router: AssetsRouter,
    interactor: AssetSearchInteractor,
    private val currencyInteractor: CurrencyInteractor,
    private val contributionsInteractor: ContributionsInteractor,
) : BaseViewModel() {

    val query = MutableStateFlow("")

    private val selectedCurrency = currencyInteractor.observeSelectCurrency()
        .inBackground()
        .share()

    private val totalContributedByAssetsFlow = contributionsInteractor.observeTotalContributedByAssets()

    val searchResults = combine(
        interactor.searchAssetsFlow(query),
        selectedCurrency,
        totalContributedByAssetsFlow
    ) { assets, currency, totalContributedByAssets ->
        assets.mapGroupedAssetsToUi(currency, totalContributedByAssets)
    }
        .distinctUntilChanged()
        .shareInBackground()

    fun cancelClicked() {
        router.back()
    }

    fun assetClicked(assetModel: AssetModel) {
        val payload = AssetPayload(
            chainId = assetModel.token.configuration.chainId,
            chainAssetId = assetModel.token.configuration.id
        )

        router.openAssetDetails(payload)
    }
}
