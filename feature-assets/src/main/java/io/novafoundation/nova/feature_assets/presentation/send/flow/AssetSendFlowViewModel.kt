package io.novafoundation.nova.feature_assets.presentation.send.flow

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.PlaceholderModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import kotlinx.coroutines.flow.Flow

class AssetSendFlowViewModel(
    interactor: AssetSearchInteractor,
    router: AssetsRouter,
    currencyInteractor: CurrencyInteractor,
    externalBalancesInteractor: ExternalBalancesInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
    resourceManager: ResourceManager,
) : AssetFlowViewModel(
    interactor,
    router,
    currencyInteractor,
    controllableAssetCheck,
    accountUseCase,
    externalBalancesInteractor,
    resourceManager,
) {

    override fun searchAssetsFlow(): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        return interactor.sendAssetSearch(query, externalBalancesFlow)
    }

    override fun assetClicked(assetModel: AssetModel) {
        val chainAsset = assetModel.token.configuration
        val assePayload = AssetPayload(chainAsset.chainId, chainAsset.id)
        router.openSend(assePayload)
    }

    override fun getPlaceholder(query: String, assets: List<Any>): PlaceholderModel? {
        if (query.isEmpty() && assets.isEmpty()) {
            return PlaceholderModel(
                text = resourceManager.getString(R.string.assets_flow_placeholder),
                imageRes = R.drawable.ic_no_search_results
            )
        } else {
            return super.getPlaceholder(query, assets)
        }
    }
}
