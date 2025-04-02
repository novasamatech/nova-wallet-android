package io.novafoundation.nova.feature_assets.presentation.trade.webInterface

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.webView.BaseWebChromeClientFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.trade.common.toTradeFlow
import io.novafoundation.nova.feature_buy_api.domain.providers.InternalProvider
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import kotlinx.coroutines.flow.combine

class TradeWebViewModel(
    private val payload: TradeWebPayload,
    private val tradeMixinFactory: TradeMixin.Factory,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val router: AssetsRouter,
    private val accountUseCase: SelectedAccountUseCase,
    private val baseWebChromeClientFactory: BaseWebChromeClientFactory
) : BaseViewModel() {

    private val tradeMixin = tradeMixinFactory.create(viewModelScope)

    private val tradeFlow = payload.type.toTradeFlow()

    private val chainFlow = flowOf { chainRegistry.getChain(payload.chainId) }

    private val chainAssetFlow = flowOf { chainRegistry.asset(payload.chainId, payload.assetId) }
        .shareInBackground()

    val integrator = combine(chainFlow, chainAssetFlow) { chain, chainAsset ->
        val address = accountUseCase.getSelectedMetaAccount().requireAddressIn(chain)
        tradeMixin.providerFor<InternalProvider>(chainAsset, tradeFlow, payload.providerId)
            .createIntegrator(chainAsset, address, tradeFlow)
    }
        .shareInBackground()

    val webChromeClientFlow = flowOf { baseWebChromeClientFactory.create(viewModelScope) }
        .shareInBackground()

    fun back() {
        router.back()
    }
}
