package io.novafoundation.nova.feature_assets.presentation.swap.asset

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.domain.interactor.AssetViewModeInteractor
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.assets.models.AssetsByViewModeResult
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractorFactory
import io.novafoundation.nova.feature_assets.domain.common.AssetBalance
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.NetworkAssetMapper
import io.novafoundation.nova.feature_assets.presentation.balance.common.mappers.TokenAssetMapper
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.flow.asset.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.swap.executor.SwapFlowExecutor
import io.novafoundation.nova.feature_assets.presentation.swap.network.NetworkSwapFlowPayload
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_swap_api.domain.interactor.SwapAvailabilityInteractor
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SwapFlowScopeAggregator
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AssetSwapFlowViewModel(
    interactorFactory: AssetSearchInteractorFactory,
    router: AssetsRouter,
    currencyInteractor: CurrencyInteractor,
    externalBalancesInteractor: ExternalBalancesInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
    resourceManager: ResourceManager,
    private val swapAvailabilityInteractor: SwapAvailabilityInteractor,
    private val swapFlowExecutor: SwapFlowExecutor,
    private val swapPayload: SwapFlowPayload,
    private val assetIconProvider: AssetIconProvider,
    assetViewModeInteractor: AssetViewModeInteractor,
    private val swapFlowScopeAggregator: SwapFlowScopeAggregator,
    private val networkAssetMapper: NetworkAssetMapper,
    private val tokenAssetMapper: TokenAssetMapper
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
    tokenAssetMapper
) {

    private val swapFlowScope = swapFlowScopeAggregator.getFlowScope(viewModelScope)

    init {
        launchInitialSwapSync()
    }

    @StringRes
    fun getTitleRes(): Int {
        return when (swapPayload) {
            SwapFlowPayload.InitialSelecting, is SwapFlowPayload.ReselectAssetIn -> R.string.assets_swap_flow_pay_title
            is SwapFlowPayload.ReselectAssetOut -> R.string.assets_swap_flow_receive_title
        }
    }

    override fun searchAssetsFlow(): Flow<AssetsByViewModeResult> {
        return interactor.searchSwapAssetsFlow(
            forAsset = swapPayload.constraintDirectionsAsset?.fullChainAssetId,
            queryFlow = query,
            externalBalancesFlow = externalBalancesFlow,
            coroutineScope = swapFlowScope
        )
    }

    override fun assetClicked(asset: Chain.Asset) {
        launch {
            swapFlowExecutor.openNextScreen(swapFlowScope, asset)
        }
    }

    override fun tokenClicked(tokenGroup: TokenGroupUi) = launchUnit {
        when (val type = tokenGroup.groupType) {
            is TokenGroupUi.GroupType.SingleItem -> assetClicked(type.asset)

            TokenGroupUi.GroupType.Group -> router.openSwapNetworks(NetworkSwapFlowPayload(NetworkFlowPayload(tokenGroup.tokenSymbol), swapPayload))
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
            tokenAssetMapper.mapTokenAssetGroupToUi(assetIconProvider, group, assets) { it.groupBalance.transferable }
        }
    }

    private fun launchInitialSwapSync() {
        if (swapPayload is SwapFlowPayload.InitialSelecting) {
            launch { swapAvailabilityInteractor.warmUpCommonlyUsedChains(swapFlowScope) }

            launch { swapAvailabilityInteractor.sync(swapFlowScope) }
        }
    }
}
