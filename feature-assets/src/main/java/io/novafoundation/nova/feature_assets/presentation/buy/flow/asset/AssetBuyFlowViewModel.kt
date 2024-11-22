package io.novafoundation.nova.feature_assets.presentation.buy.flow.asset

import io.novafoundation.nova.common.domain.interactor.AssetViewModeInteractor
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.models.AssetsByViewModeResult
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractorFactory
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.flow.asset.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixin
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountFormatter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
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
    assetIconProvider: AssetIconProvider,
    assetViewModeInteractor: AssetViewModeInteractor,
    amountFormatter: AmountFormatter
) : AssetFlowViewModel(
    interactorFactory,
    router,
    currencyInteractor,
    controllableAssetCheck,
    accountUseCase,
    externalBalancesInteractor,
    resourceManager,
    assetIconProvider,
    assetViewModeInteractor,
    amountFormatter
) {

    val buyMixin = buyMixinFactory.create(scope = this)

    override fun searchAssetsFlow(): Flow<AssetsByViewModeResult> {
        return interactor.buyAssetSearch(query, externalBalancesFlow)
    }

    override fun assetClicked(asset: Chain.Asset) {
        validate(asset) {
            buyMixin.buyClicked(asset)
        }
    }

    override fun tokenClicked(tokenGroup: TokenGroupUi) {
        when (val type = tokenGroup.groupType) {
            is TokenGroupUi.GroupType.SingleItem -> assetClicked(type.asset)
            TokenGroupUi.GroupType.Group -> router.openBuyNetworks(NetworkFlowPayload(tokenGroup.tokenSymbol))
        }
    }
}