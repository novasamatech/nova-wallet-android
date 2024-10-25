package io.novafoundation.nova.feature_assets.presentation.swap.network

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.common.PricedAmount
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.networks.AssetNetworksInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.flow.network.model.NetworkFlowRvItem
import io.novafoundation.nova.feature_assets.presentation.swap.executor.SwapFlowExecutor
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NetworkSwapFlowViewModel(
    interactor: AssetNetworksInteractor,
    router: AssetsRouter,
    externalBalancesInteractor: ExternalBalancesInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
    resourceManager: ResourceManager,
    networkFlowPayload: NetworkFlowPayload,
    chainRegistry: ChainRegistry,
    private val swapFlowExecutor: SwapFlowExecutor
) : NetworkFlowViewModel(
    interactor,
    router,
    controllableAssetCheck,
    accountUseCase,
    externalBalancesInteractor,
    resourceManager,
    networkFlowPayload,
    chainRegistry
) {

    override fun getAssetBalance(asset: AssetWithNetwork): PricedAmount {
        return asset.balanceWithOffChain.transferable
    }

    override fun assetsFlow(tokenSymbol: TokenSymbol): Flow<List<AssetWithNetwork>> {
        return interactor.swapAssetsFlow(tokenSymbol, externalBalancesFlow)
    }

    override fun networkClicked(network: NetworkFlowRvItem) {
        launch {
            val chainAsset = chainRegistry.asset(network.chainId, network.assetId)
            swapFlowExecutor.openNextScreen(coroutineScope, chainAsset)
        }
    }

    override fun getTitle(tokenSymbol: TokenSymbol): String {
        return resourceManager.getString(R.string.swap_network_flow_title, tokenSymbol.value)
    }
}
