package io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.transak

import android.util.Log
import android.webkit.JavascriptInterface
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener
import org.json.JSONObject

class TransakJsEventBridge(
    private val closeListener: OnTradeOperationFinishedListener,
    private val tradeSellCallback: OnSellOrderCreatedListener
) {

    @JavascriptInterface
    fun onTransakEvent(eventData: String) {
        val json = JSONObject(eventData)
        val eventId = json.getString("event_id")
        Log.d("TransakEvent", "Event: $eventId, Data: $eventData")

        when (eventId) {
            "TRANSAK_WIDGET_CLOSE" -> closeListener.onTradeOperationFinished()

            "TRANSAK_ORDER_CREATED" -> {
                val json = JSONObject(eventData).getJSONObject("data")
                if (json.getString("isBuyOrSell") == "SELL") {
                    tradeSellCallback.onSellOrderCreated(
                        json.getString("id"),
                        json.getJSONObject("cryptoPaymentData").getString("paymentAddress"),
                        json.getString("cryptoAmount").toBigDecimal()
                    )
                }
            }
        }
    }
}
