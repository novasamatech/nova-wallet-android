package io.novafoundation.nova.feature_assets.presentation.flow.network

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.ExternalBalancesInteractor
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.PricedAmount
import io.novafoundation.nova.feature_assets.domain.networks.AssetNetworksInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.flow.network.model.NetworkFlowRvItem
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

abstract class NetworkFlowViewModel(
    protected val interactor: AssetNetworksInteractor,
    protected val router: AssetsRouter,
    private val controllableAssetCheck: ControllableAssetCheckMixin,
    internal val accountUseCase: SelectedAccountUseCase,
    externalBalancesInteractor: ExternalBalancesInteractor,
    internal val resourceManager: ResourceManager,
    private val networkFlowPayload: NetworkFlowPayload,
    internal val chainRegistry: ChainRegistry
) : BaseViewModel() {

    val acknowledgeLedgerWarning = controllableAssetCheck.acknowledgeLedgerWarning

    protected val externalBalancesFlow = externalBalancesInteractor.observeExternalBalances()

    val titleFlow: Flow<String> = flowOf { getTitle(networkFlowPayload.asTokenSymbol()) }
    val networks: Flow<List<NetworkFlowRvItem>> = assetsFlow(networkFlowPayload.asTokenSymbol())
        .map { mapAssets(it) }

    abstract fun getAssetBalance(asset: AssetWithNetwork): PricedAmount

    abstract fun assetsFlow(tokenSymbol: TokenSymbol): Flow<List<AssetWithNetwork>>

    abstract fun networkClicked(network: NetworkFlowRvItem)

    abstract fun getTitle(tokenSymbol: TokenSymbol): String

    fun backClicked() {
        router.back()
    }

    internal fun validate(networkFlowRvItem: NetworkFlowRvItem, onAccept: () -> Unit) {
        launch {
            val metaAccount = accountUseCase.getSelectedMetaAccount()
            val chainAsset = chainRegistry.asset(networkFlowRvItem.chainId, networkFlowRvItem.assetId)
            controllableAssetCheck.check(metaAccount, chainAsset) {
                onAccept()
            }
        }
    }

    private fun mapAssets(assetWithNetworks: List<AssetWithNetwork>): List<NetworkFlowRvItem> {
        return assetWithNetworks
            .map {
                NetworkFlowRvItem(
                    it.chain.id,
                    it.asset.token.configuration.id,
                    it.chain.name,
                    it.chain.icon,
                    mapAmountToAmountModel(
                        amount = getAssetBalance(it).amount,
                        asset = it.asset,
                        includeAssetTicker = false
                    )
                )
            }
    }
}
