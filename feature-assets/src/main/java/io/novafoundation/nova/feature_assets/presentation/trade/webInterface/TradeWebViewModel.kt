package io.novafoundation.nova.feature_assets.presentation.trade.webInterface

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.webView.BaseWebChromeClientFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressPayload
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressRequester
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressResponder
import io.novafoundation.nova.feature_assets.presentation.trade.common.TradeProviderFlowType
import io.novafoundation.nova.feature_assets.presentation.trade.common.toTradeFlow
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.providers.WebViewIntegrationProvider
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import java.math.BigDecimal
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TradeWebViewModel(
    private val payload: TradeWebPayload,
    private val tradeMixinFactory: TradeMixin.Factory,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val router: AssetsRouter,
    private val accountUseCase: SelectedAccountUseCase,
    private val baseWebChromeClientFactory: BaseWebChromeClientFactory,
    private val topUpRequester: TopUpAddressRequester,
) : BaseViewModel(), OnTradeOperationFinishedListener, OnSellOrderCreatedListener {

    private val tradeMixin = tradeMixinFactory.create(viewModelScope)

    private val tradeFlow = payload.type.toTradeFlow()

    private val chainFlow = flowOf { chainRegistry.getChain(payload.asset.chainId) }

    private val chainAssetFlow = flowOf { chainRegistry.asset(payload.asset.chainId, payload.asset.chainAssetId) }
        .shareInBackground()

    val integrator = combine(chainFlow, chainAssetFlow) { chain, chainAsset ->
        val address = accountUseCase.getSelectedMetaAccount().requireAddressIn(chain)
        tradeMixin.providerFor<WebViewIntegrationProvider>(chainAsset, tradeFlow, payload.providerId)
            .createIntegrator(
                chainAsset = chainAsset,
                address = address,
                tradeFlow = tradeFlow,
                onCloseListener = this,
                onSellOrderCreatedListener = this
            )
    }
        .shareInBackground()

    val webChromeClientFlow = flowOf { baseWebChromeClientFactory.create(viewModelScope) }
        .shareInBackground()

    fun back() {
        router.back()
    }

    init {
        observeTopUp()
    }

    override fun onTradeOperationFinished(success: Boolean) = launchUnit {
        if (success) {
            val messageResId = when (payload.type) {
                TradeProviderFlowType.BUY -> R.string.buy_order_completed_message
                TradeProviderFlowType.SELL -> R.string.sell_order_completed_message
            }
            showToast(resourceManager.getString(messageResId))

            router.finishTradeOperation(payload.asset)
        } else {
            router.returnToMainScreen()
        }
    }

    override fun onSellOrderCreated(orderId: String, address: String, amount: BigDecimal) = launchUnit {
        val asset = chainAssetFlow.first()

        val request = TopUpAddressPayload(
            address,
            amount,
            payload.asset,
            screenTitle = resourceManager.getString(R.string.fragment_sell_token_title, asset.symbol.value)
        )

        topUpRequester.openRequest(request)
    }

    private fun observeTopUp() {
        topUpRequester.responseFlow
            .onEach {
                if (it == TopUpAddressResponder.Response.Cancel) {
                    router.returnToMainScreen()
                }
            }
            .launchIn(this)
    }
}
