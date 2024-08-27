package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import java.math.BigDecimal

interface NovaCardEventHandler {

    fun transactionStatusChanged(event: TransactionStatus)

    fun openTopUp(amount: BigDecimal, address: String)

    enum class TransactionStatus {
        NEW,
        PENDING,
        COMPLETED,
        UNKNOWN
    }
}
