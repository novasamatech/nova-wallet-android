package io.novafoundation.nova.feature_buy_impl.presentation.common

import java.util.UUID

fun generateMerchantTransactionId(): String {
    return UUID.randomUUID().toString()
}
