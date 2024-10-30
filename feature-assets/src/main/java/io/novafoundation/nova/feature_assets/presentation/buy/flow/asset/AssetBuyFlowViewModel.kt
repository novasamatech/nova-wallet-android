package io.novafoundation.nova.feature_assets.presentation.buy.flow.asset

import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.models.AssetFlowSearchResult
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractorFactory
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.flow.asset.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixin
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import kotlinx.coroutines.flow.Flow

class AssetBuyFlowViewModel(
    interactorFactory: AssetSearchInteractorFactory,
    router: AssetsRouter,
    externalBalancesInteractor: ExternalBalancesInteractor,
    currencyInteractor: CurrencyInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
    buyMixinFactory: BuyMixin.Factory,
    resourceManager: ResourceManager,
    assetIconProvider: AssetIconProvider
) : AssetFlowViewModel(
    interactorFactory,
    router,
    currencyInteractor,
    controllableAssetCheck,
    accountUseCase,
    externalBalancesInteractor,
    resourceManager,
    assetIconProvider
) {

    val buyMixin = buyMixinFactory.create(scope = this)

    override fun searchAssetsFlow(): Flow<AssetFlowSearchResult> {
        return interactor.buyAssetSearch(query, externalBalancesFlow)
    }

    override fun assetClicked(assetModel: AssetModel) {
        validate(assetModel) {
            val chainAsset = assetModel.token.configuration
            buyMixin.buyClicked(chainAsset)
        }
    }

    override fun tokenClicked(tokenGroup: TokenGroupUi) {
        when (val type = tokenGroup.groupType) {
            is TokenGroupUi.GroupType.SingleItem -> assetClicked(type.asset)
            TokenGroupUi.GroupType.Group -> router.openBuyNetworks(NetworkFlowPayload(tokenGroup.tokenSymbol))
        }
    }
}
