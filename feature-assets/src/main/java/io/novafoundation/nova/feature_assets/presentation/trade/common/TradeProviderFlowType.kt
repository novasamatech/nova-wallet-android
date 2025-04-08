package io.novafoundation.nova.feature_assets.presentation.trade.common

import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry

enum class TradeProviderFlowType {
    BUY, SELL
}

fun TradeProviderFlowType.toTradeType() = when (this) {
    TradeProviderFlowType.BUY -> TradeTokenRegistry.TradeType.BUY
    TradeProviderFlowType.SELL -> TradeTokenRegistry.TradeType.SELL
}

fun TradeTokenRegistry.TradeType.toModel() = when (this) {
    TradeTokenRegistry.TradeType.BUY -> TradeProviderFlowType.BUY
    TradeTokenRegistry.TradeType.SELL -> TradeProviderFlowType.SELL
}
