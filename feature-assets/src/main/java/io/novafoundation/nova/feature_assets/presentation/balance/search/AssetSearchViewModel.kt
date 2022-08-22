package io.novafoundation.nova.feature_assets.presentation.balance.search

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AssetSearchViewModel(
    private val router: AssetsRouter,
    interactor: AssetSearchInteractor,
) : BaseViewModel() {

    val query = MutableStateFlow("")

    val searchResults = interactor.searchAssetsFlow(query)
        .mapGroupedAssetsToUi()
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
