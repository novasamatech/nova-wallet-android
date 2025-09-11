package io.novafoundation.nova.feature_assets.presentation.receive.flow.asset

import io.novafoundation.nova.common.domain.interactor.AssetViewModeInteractor
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.models.AssetsByViewModeResult
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.flow.asset.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.maskable.MaskableAmountFormatter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

class AssetReceiveFlowViewModel(
    interactorFactory: AssetSearchInteractorFactory,
    router: AssetsRouter,
    currencyInteractor: CurrencyInteractor,
    externalBalancesInteractor: ExternalBalancesInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
    resourceManager: ResourceManager,
    assetIconProvider: AssetIconProvider,
    assetViewModeInteractor: AssetViewModeInteractor,
    amountFormatter: MaskableAmountFormatter
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
        return interactor.searchReceiveAssetsFlow(query, externalBalancesFlow)
    }

    override fun assetClicked(asset: Chain.Asset) {
        validate(asset) {
            openNextScreen(asset)
        }
    }

    override fun tokenClicked(tokenGroup: TokenGroupUi) {
        when (val type = tokenGroup.groupType) {
            is TokenGroupUi.GroupType.SingleItem -> assetClicked(type.asset)

            TokenGroupUi.GroupType.Group -> router.openReceiveNetworks(NetworkFlowPayload(tokenGroup.tokenSymbol))
        }
    }

    private fun openNextScreen(asset: Chain.Asset) {
        val assetPayload = AssetPayload(asset.chainId, asset.id)
        router.openReceive(assetPayload)
    }
}
