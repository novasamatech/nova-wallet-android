package io.novafoundation.nova.feature_assets.presentation.trade.provider

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.trade.common.TradeProviderFlowType
import io.novafoundation.nova.feature_assets.presentation.trade.common.toModel
import io.novafoundation.nova.feature_assets.presentation.trade.webInterface.TradeWebPayload
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TradeProviderListViewModel(
    private val payload: TradeProviderListPayload,
    private val tradeMixinFactory: TradeMixin.Factory,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val router: AssetsRouter,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
) : BaseViewModel() {

    val confirmationAwaitableAction = actionAwaitableMixinFactory.confirmingAction<ConfirmationDialogInfo>()

    private val tradeMixin = tradeMixinFactory.create(viewModelScope)

    private val tradeType = payload.type.toTradeFlow()

    private val chainAssetFlow = flowOf { chainRegistry.asset(payload.chainId, payload.assetId) }
        .shareInBackground()

    val titleFlow = chainAssetFlow.map {
        when (tradeType) {
            TradeTokenRegistry.TradeType.BUY -> resourceManager.getString(R.string.trade_provider_list_buy_title, it.symbol.value)
            TradeTokenRegistry.TradeType.SELL -> resourceManager.getString(R.string.trade_provider_list_sell_title, it.symbol.value)
        }
    }

    private val providers = chainAssetFlow.map {
        tradeMixin.providersFor(it, tradeType)
    }.shareInBackground()

    val providerModels = providers.mapList { provider ->
        val paymentMethods = provider.getPaymentMethods(tradeType).map { it.toModel() }
        TradeProviderRvItem(
            provider.id,
            provider.officialUrl,
            provider.logoRes,
            paymentMethods,
            resourceManager.getString(provider.getDescriptionRes(tradeType))
        )
    }

    fun back() {
        router.back()
    }

    private fun TradeProviderFlowType.toTradeFlow() = when (this) {
        TradeProviderFlowType.BUY -> TradeTokenRegistry.TradeType.BUY
        TradeProviderFlowType.SELL -> TradeTokenRegistry.TradeType.SELL
    }

    private fun TradeTokenRegistry.PaymentMethod.toModel() = when (this) {
        TradeTokenRegistry.PaymentMethod.ApplePay -> TradeProviderRvItem.PaymentMethod.ByResId(R.drawable.ic_apple_pay)
        TradeTokenRegistry.PaymentMethod.BankTransfer -> TradeProviderRvItem.PaymentMethod.ByResId(R.drawable.ic_bank)
        TradeTokenRegistry.PaymentMethod.GooglePay -> TradeProviderRvItem.PaymentMethod.ByResId(R.drawable.ic_google_pay)
        TradeTokenRegistry.PaymentMethod.MasterCard -> TradeProviderRvItem.PaymentMethod.ByResId(R.drawable.ic_mastercard)
        TradeTokenRegistry.PaymentMethod.Sepa -> TradeProviderRvItem.PaymentMethod.ByResId(R.drawable.ic_sepa)
        TradeTokenRegistry.PaymentMethod.Visa -> TradeProviderRvItem.PaymentMethod.ByResId(R.drawable.ic_visa)

        is TradeTokenRegistry.PaymentMethod.Other -> TradeProviderRvItem.PaymentMethod.ByText(
            resourceManager.getString(
                R.string.additional_payment_methods,
                this.quantity
            )
        )
    }

    fun onProviderClicked(item: TradeProviderRvItem) {
        launch {
            awaitConfirmation(item)

            val chainAsset = chainAssetFlow.first()

            router.openTradeWebInterface(
                TradeWebPayload(
                    AssetPayload(chainAsset.chainId, chainAsset.id),
                    item.providerId,
                    tradeType.toModel(),
                    payload.onSuccessfulTradeStrategyType
                )
            )
        }
    }

    private suspend fun awaitConfirmation(item: TradeProviderRvItem) {
        confirmationAwaitableAction.awaitAction(
            ConfirmationDialogInfo(
                resourceManager.getString(R.string.trade_provider_open_confirmation_title),
                resourceManager.getString(R.string.trade_provider_open_confirmation_message, item.providerLink),
                resourceManager.getString(R.string.common_continue),
                resourceManager.getString(R.string.common_cancel)
            )
        )
    }
}
