package io.novafoundation.nova.feature_assets.presentation.trade.sell.flow.network

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.common.AssetBalance
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.networks.AssetNetworksInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.flow.network.model.NetworkFlowRvItem
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow

class NetworkSellFlowViewModel(
    interactor: AssetNetworksInteractor,
    router: AssetsRouter,
    externalBalancesInteractor: ExternalBalancesInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
    resourceManager: ResourceManager,
    networkFlowPayload: NetworkFlowPayload,
    chainRegistry: ChainRegistry,
    amountFormatter: AmountFormatter
) : NetworkFlowViewModel(
    interactor,
    router,
    controllableAssetCheck,
    accountUseCase,
    externalBalancesInteractor,
    resourceManager,
    networkFlowPayload,
    chainRegistry,
    amountFormatter
) {

    override fun getAssetBalance(asset: AssetWithNetwork): AssetBalance.Amount {
        return asset.balanceWithOffChain.total
    }

    override fun assetsFlow(tokenSymbol: TokenSymbol): Flow<List<AssetWithNetwork>> {
        return interactor.tradeAssetFlow(tokenSymbol, externalBalancesFlow, TradeTokenRegistry.TradeType.SELL)
    }

    override fun networkClicked(network: NetworkFlowRvItem) {
        validateControllsAsset(network) {
            router.openSellProviders(network.chainId, network.assetId)
        }
    }

    override fun getTitle(tokenSymbol: TokenSymbol): String {
        return resourceManager.getString(R.string.sell_network_flow_title, tokenSymbol.value)
    }
}
