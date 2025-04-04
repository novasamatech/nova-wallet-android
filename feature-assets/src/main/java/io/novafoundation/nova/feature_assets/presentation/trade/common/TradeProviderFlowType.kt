package io.novafoundation.nova.feature_assets.presentation.trade.common

import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry

enum class TradeProviderFlowType {
    BUY, SELL
}

fun TradeProviderFlowType.toTradeFlow() = when (this) {
    TradeProviderFlowType.BUY -> TradeTokenRegistry.TradeFlow.BUY
    TradeProviderFlowType.SELL -> TradeTokenRegistry.TradeFlow.SELL
}

fun TradeTokenRegistry.TradeFlow.toModel() = when (this) {
    TradeTokenRegistry.TradeFlow.BUY -> TradeProviderFlowType.BUY
    TradeTokenRegistry.TradeFlow.SELL -> TradeProviderFlowType.SELL
}
