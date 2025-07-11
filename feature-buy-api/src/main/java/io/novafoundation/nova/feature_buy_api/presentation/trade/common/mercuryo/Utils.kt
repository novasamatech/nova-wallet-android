package io.novafoundation.nova.feature_buy_api.presentation.trade.common.mercuryo

import java.util.UUID

fun generateMerchantTransactionId(): String {
    return UUID.randomUUID().toString()
}
