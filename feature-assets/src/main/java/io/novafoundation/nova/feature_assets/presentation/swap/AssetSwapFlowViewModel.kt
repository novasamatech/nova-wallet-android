package io.novafoundation.nova.feature_assets.presentation.swap

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.mapGroupedAssetsToUi
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_assets.presentation.swap.executor.SwapFlowExecutor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AssetSwapFlowViewModel(
    interactor: AssetSearchInteractor,
    router: AssetsRouter,
    currencyInteractor: CurrencyInteractor,
    externalBalancesInteractor: ExternalBalancesInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
    resourceManager: ResourceManager,
    private val swapAvailabilityInteractor: SwapAvailabilityInteractor,
    private val swapFlowExecutor: SwapFlowExecutor,
    private val payload: SwapFlowPayload
) : AssetFlowViewModel(
    interactor,
    router,
    currencyInteractor,
    controllableAssetCheck,
    accountUseCase,
    externalBalancesInteractor,
    resourceManager,
) {

    init {
        launch {
            if (payload is SwapFlowPayload.InitialSelecting) {
                swapAvailabilityInteractor.sync(viewModelScope)
            }
        }

        launch {
            if (payload is SwapFlowPayload.InitialSelecting) {
                interactor.warmUpSwapCommonlyUsedChains(viewModelScope)
            }
        }
    }

    @StringRes
    fun getTitleRes(): Int {
        return when (payload) {
            SwapFlowPayload.InitialSelecting, is SwapFlowPayload.ReselectAssetIn -> R.string.assets_swap_flow_pay_title
            is SwapFlowPayload.ReselectAssetOut -> R.string.assets_swap_flow_receive_title
        }
    }

    override fun searchAssetsFlow(): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        return interactor.searchSwapAssetsFlow(
            forAsset = payload.constraintDirectionsAsset?.fullChainAssetId,
            queryFlow = query,
            externalBalancesFlow = externalBalancesFlow,
            coroutineScope = viewModelScope
        )
    }

    override fun assetClicked(assetModel: AssetModel) {
        launch {
            val chainAsset = assetModel.token.configuration
            swapFlowExecutor.openNextScreen(viewModelScope, chainAsset)
        }
    }

    override fun mapAssets(assets: Map<AssetGroup, List<AssetWithOffChainBalance>>, currency: Currency): List<Any> {
        return assets.mapGroupedAssetsToUi(currency, AssetGroup::groupTransferableBalanceFiat, AssetWithOffChainBalance.Balance::transferable)
    }
}
