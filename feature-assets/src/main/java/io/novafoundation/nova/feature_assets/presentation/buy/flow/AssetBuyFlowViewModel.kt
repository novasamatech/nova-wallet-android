package io.novafoundation.nova.feature_assets.presentation.buy.flow

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixin
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import kotlinx.coroutines.flow.Flow

class AssetBuyFlowViewModel(
    interactor: AssetSearchInteractor,
    router: AssetsRouter,
    externalBalancesInteractor: ExternalBalancesInteractor,
    currencyInteractor: CurrencyInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
    buyMixinFactory: BuyMixin.Factory,
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

    val buyMixin = buyMixinFactory.create(scope = this)

    override fun searchAssetsFlow(): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        return interactor.buyAssetSearch(query, externalBalancesFlow)
    }

    override fun assetClicked(assetModel: AssetModel) {
        validate(assetModel) {
            val chainAsset = assetModel.token.configuration
            buyMixin.buyClicked(chainAsset)
        }
    }
}
