package io.novafoundation.nova.feature_assets.presentation.buy.flow

import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy.BuyMixinFactory
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import kotlinx.coroutines.flow.Flow

class AssetBuyFlowViewModel(
    router: AssetsRouter,
    interactor: AssetSearchInteractor,
    currencyInteractor: CurrencyInteractor,
    contributionsInteractor: ContributionsInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
    buyMixinFactory: BuyMixinFactory
) : AssetFlowViewModel(
    router,
    interactor,
    currencyInteractor,
    contributionsInteractor,
    controllableAssetCheck,
    accountUseCase
) {

    val buyMixin = buyMixinFactory.create(scope = this)

    override fun searchAssetsFlow(): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        return interactor.buyAssetSearch(query, totalContributedByAssetsFlow)
    }

    override fun openNextScreen(assetModel: AssetModel) {
        val chainAsset = assetModel.token.configuration
        buyMixin.buyClicked(chainAsset)
    }
}
