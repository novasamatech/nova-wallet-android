package io.novafoundation.nova.feature_assets.presentation.gifts.assets

import io.novafoundation.nova.common.domain.interactor.AssetViewModeInteractor
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
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
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.NetworkAssetFormatter
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.TokenAssetFormatter
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.flow.asset.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

class AssetGiftsFlowViewModel(
    interactorFactory: AssetSearchInteractorFactory,
    router: AssetsRouter,
    currencyInteractor: CurrencyInteractor,
    externalBalancesInteractor: ExternalBalancesInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
    resourceManager: ResourceManager,
    private val assetIconProvider: AssetIconProvider,
    assetViewModeInteractor: AssetViewModeInteractor,
    private val networkAssetMapper: NetworkAssetFormatter,
    private val tokenAssetFormatter: TokenAssetFormatter
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
    networkAssetMapper,
    tokenAssetFormatter
) {

    override fun searchAssetsFlow(): Flow<AssetsByViewModeResult> {
        return interactor.giftAssetsSearch(query, externalBalancesFlow)
    }

    override fun assetClicked(asset: Chain.Asset) {
        val assetPayload = AssetPayload(asset.chainId, asset.id)
        router.openSelectGiftAmount(assetPayload)
    }

    override fun tokenClicked(tokenGroup: TokenGroupUi) {
        when (val type = tokenGroup.groupType) {
            is TokenGroupUi.GroupType.SingleItem -> assetClicked(type.asset)

            TokenGroupUi.GroupType.Group -> router.openGiftsNetworks(NetworkFlowPayload(tokenGroup.tokenSymbol))
        }
    }

    override fun mapNetworkAssets(assets: Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>, currency: Currency): List<BalanceListRvItem> {
        return networkAssetMapper.mapGroupedAssetsToUi(
            assets,
            assetIconProvider,
            currency,
            NetworkAssetGroup::groupTransferableBalanceFiat,
            AssetBalance::transferable
        )
    }

    override fun mapTokensAssets(assets: Map<TokenAssetGroup, List<AssetWithNetwork>>): List<BalanceListRvItem> {
        return assets.map { (group, assets) ->
            tokenAssetFormatter.mapTokenAssetGroupToUi(assetIconProvider, group, assets = assets) { it.groupBalance.transferable }
        }
    }
}
