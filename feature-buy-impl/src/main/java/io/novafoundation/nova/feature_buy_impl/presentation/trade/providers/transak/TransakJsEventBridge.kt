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
    fun postMessage(eventData: String) {
        val json = JSONObject(eventData)
        val eventId = json.getString("event_id")
        Log.d("TransakEvent", "Event: $eventId, Data: $eventData")

        val data = json.get("data")
        when (eventId) {
            "TRANSAK_WIDGET_CLOSE" -> {
                val isOrderSuccessful = data == true // For unsuccessful order data is JSONObject
                closeListener.onTradeOperationFinished(isOrderSuccessful)
            }

            "TRANSAK_ORDER_CREATED" -> {
                require(data is JSONObject)
                if (data.getString("isBuyOrSell") == "SELL") {
                    tradeSellCallback.onSellOrderCreated(
                        data.getString("id"),
                        data.getJSONObject("cryptoPaymentData").getString("paymentAddress"),
                        data.getString("cryptoAmount").toBigDecimal()
                    )
                }
            }
        }
    }
}
