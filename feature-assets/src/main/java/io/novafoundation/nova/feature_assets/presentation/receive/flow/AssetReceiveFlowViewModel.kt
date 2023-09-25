package io.novafoundation.nova.feature_assets.presentation.receive.flow

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
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

class AssetReceiveFlowViewModel(
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
        return interactor.searchAssetsFlow(query, externalBalancesFlow)
    }

    override fun assetClicked(assetModel: AssetModel) {
        validate(assetModel) {
            openNextScreen(assetModel)
        }
    }

    private fun openNextScreen(assetModel: AssetModel) {
        val chainAsset = assetModel.token.configuration
        val assePayload = AssetPayload(chainAsset.chainId, chainAsset.id)
        router.openReceive(assePayload)
    }
}
