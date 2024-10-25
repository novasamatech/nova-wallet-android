package io.novafoundation.nova.feature_assets.presentation.send.flow

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.PlaceholderModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.AssetBalance
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
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

    override fun searchAssetsFlow(): Flow<Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>> {
        return interactor.sendAssetSearch(query, externalBalancesFlow)
    }

    override fun assetClicked(assetModel: AssetModel) {
        val chainAsset = assetModel.token.configuration
        val assetPayload = AssetPayload(chainAsset.chainId, chainAsset.id)
        router.openSend(SendPayload.SpecifiedOrigin(assetPayload))
    }

    override fun mapAssets(assets: Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>, currency: Currency): List<BalanceListRvItem> {
        return assets.mapGroupedAssetsToUi(currency, NetworkAssetGroup::groupTransferableBalanceFiat, AssetBalance::transferable)
    }

    override fun getPlaceholder(query: String, assets: List<Any>): PlaceholderModel? {
        if (query.isEmpty() && assets.isEmpty()) {
            return PlaceholderModel(
                text = resourceManager.getString(R.string.assets_send_flow_placeholder),
                imageRes = R.drawable.ic_no_search_results,
                buttonText = resourceManager.getString(R.string.assets_buy_tokens_placeholder_button),
            )
        } else {
            return super.getPlaceholder(query, assets)
        }
    }

    fun openBuyFlow() {
        router.openBuyFlowFromSendFlow()
    }
}
