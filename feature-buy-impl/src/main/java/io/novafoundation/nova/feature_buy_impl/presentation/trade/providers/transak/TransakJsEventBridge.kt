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
    fun onTransakEvent(eventId: String, eventData: String) {
        val json = JSONObject(eventData).getJSONObject("data")
        Log.d("TransakEvent", "Event: $eventId, Data: $json")

        when (eventId) {
            "TRANSAK_WIDGET_CLOSE" -> closeListener.onTradeOperationFinished()

            "TRANSAK_ORDER_CREATED" -> {
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
