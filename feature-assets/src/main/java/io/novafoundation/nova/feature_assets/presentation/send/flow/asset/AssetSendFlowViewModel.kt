package io.novafoundation.nova.feature_assets.presentation.send.flow.asset

import io.novafoundation.nova.common.domain.interactor.AssetViewModeInteractor
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.PlaceholderModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.models.AssetsByViewModeResult
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractorFactory
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetBalance
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.mapTokenAssetGroupToUi
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.flow.asset.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_assets.presentation.send.amount.SendPayload
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import kotlinx.coroutines.flow.Flow

class AssetSendFlowViewModel(
    interactorFactory: AssetSearchInteractorFactory,
    router: AssetsRouter,
    currencyInteractor: CurrencyInteractor,
    externalBalancesInteractor: ExternalBalancesInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
    resourceManager: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    assetViewModeInteractor: AssetViewModeInteractor,
    private val amountFormatter: AmountFormatter
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

    override fun searchAssetsFlow(): Flow<AssetsByViewModeResult> {
        return interactor.sendAssetSearch(query, externalBalancesFlow)
    }

    override fun assetClicked(assetModel: AssetModel) {
        val chainAsset = assetModel.token.configuration
        val assetPayload = AssetPayload(chainAsset.chainId, chainAsset.id)
        router.openSend(SendPayload.SpecifiedOrigin(assetPayload))
    }

    override fun tokenClicked(tokenGroup: TokenGroupUi) {
        when (val type = tokenGroup.groupType) {
            is TokenGroupUi.GroupType.SingleItem -> assetClicked(type.asset)

            TokenGroupUi.GroupType.Group -> router.openSendNetworks(NetworkFlowPayload(tokenGroup.tokenSymbol))
        }
    }

    override fun mapNetworkAssets(assets: Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>, currency: Currency): List<BalanceListRvItem> {
        return assets.mapGroupedAssetsToUi(
            amountFormatter,
            assetIconProvider,
            currency,
            NetworkAssetGroup::groupTransferableBalanceFiat,
            AssetBalance::transferable
        )
    }

    override fun mapTokensAssets(assets: Map<TokenAssetGroup, List<AssetWithNetwork>>): List<BalanceListRvItem> {
        return assets.map { (group, assets) ->
            mapTokenAssetGroupToUi(amountFormatter, assetIconProvider, group, assets = assets) { it.groupBalance.transferable }
        }
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
