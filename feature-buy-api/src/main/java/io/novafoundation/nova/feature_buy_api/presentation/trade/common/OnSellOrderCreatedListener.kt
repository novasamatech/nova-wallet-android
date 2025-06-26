package io.novafoundation.nova.feature_buy_api.presentation.trade.common

import java.math.BigDecimal

interface OnSellOrderCreatedListener {
    fun onSellOrderCreated(orderId: String, address: String, amount: BigDecimal)
}
