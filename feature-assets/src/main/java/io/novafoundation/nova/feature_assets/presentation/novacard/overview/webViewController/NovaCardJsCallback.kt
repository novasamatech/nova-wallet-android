package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import android.webkit.JavascriptInterface
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.fromJson
import java.math.BigDecimal

class NovaCardJsCallback(
    private val gson: Gson,
    private val eventHandler: NovaCardEventHandler
) {
    @JavascriptInterface
    fun onStatusChange(data: String) {
        runCatching {
            val event = gson.fromJson<StatusChangedEvent>(data)

            val transactionStatus = when (event.status) {
                "new" -> NovaCardEventHandler.TransactionStatus.NEW
                "pending" -> NovaCardEventHandler.TransactionStatus.PENDING
                "completed" -> NovaCardEventHandler.TransactionStatus.COMPLETED
                else -> NovaCardEventHandler.TransactionStatus.UNKNOWN
            }

            eventHandler.transactionStatusChanged(transactionStatus)
        }
    }

    @JavascriptInterface
    fun onSellTransferEnabled(data: String) {
        runCatching {
            val event = gson.fromJson<SellTransferEnabledEvent>(data)

            eventHandler.openTopUp(event.amount, event.address)
        }
    }
}

// { id:"0c8302ebd6a635181", status:"completed", amount:"6.5579913810", network:"POLKADOT", fiat_amount:"25.00", currency:"DOT", fiat_currency:"EUR" }
private class StatusChangedEvent(val status: String)

// { amount:"0.01336", currency:"BTC", network:"BITCOIN", address:"04d3911f3b6de0843", id:"03b22d25d523a5285", flow_id:"payout" }
private class SellTransferEnabledEvent(val address: String, val amount: BigDecimal)
