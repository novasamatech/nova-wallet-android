package io.novafoundation.nova.feature_assets.presentation.balance.search

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ExpandableAssetsMixinFactory
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow

class AssetSearchViewModel(
    private val router: AssetsRouter,
    interactorFactory: AssetSearchInteractorFactory,
    externalBalancesInteractor: ExternalBalancesInteractor,
    expandableAssetsMixinFactory: ExpandableAssetsMixinFactory
) : BaseViewModel() {

    val interactor = interactorFactory.createByAssetViewMode()

    val query = MutableStateFlow("")

    private val externalBalances = externalBalancesInteractor.observeExternalBalances()

    private val assetsFlow = interactor.searchAssetsFlow(query, externalBalances)

    val assetListMixin = expandableAssetsMixinFactory.create(assetsFlow)

    val searchResults = assetListMixin.assetModelsFlow

    fun cancelClicked() {
        router.back()
    }

    fun assetClicked(asset: Chain.Asset) {
        val payload = AssetPayload(
            chainId = asset.chainId,
            chainAssetId = asset.id
        )

        router.openAssetDetails(payload)
    }
}
