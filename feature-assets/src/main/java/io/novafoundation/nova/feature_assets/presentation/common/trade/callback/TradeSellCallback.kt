package io.novafoundation.nova.feature_assets.presentation.common.trade.callback

import java.math.BigDecimal

interface TradeSellCallback {

    fun onSellCompleted(orderId: String)

    fun onSellStart(orderId: String, amount: BigDecimal, address: String)
}
