package io.novafoundation.nova.feature_buy_api.domain.common

import java.math.BigDecimal

interface OnSellOrderCreatedListener {
    fun onSellOrderCreated(orderId: String, address: String, amount: BigDecimal)
}
