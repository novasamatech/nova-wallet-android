package io.novafoundation.nova.feature_assets.presentation.common.trade.mercuryo

import android.webkit.WebResourceRequest
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.webView.WebViewRequestInterceptor
import io.novafoundation.nova.common.utils.webView.makeRequestBlocking
import io.novafoundation.nova.common.utils.webView.toOkHttpRequestBuilder
import io.novafoundation.nova.feature_assets.presentation.common.trade.callback.TradeBuyCallback
import io.novafoundation.nova.feature_assets.presentation.common.trade.callback.TradeSellCallback
import okhttp3.OkHttpClient

class MercuryoBuyRequestInterceptorFactory(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    fun create(tradeSellCallback: TradeSellCallback): MercuryoSellRequestInterceptor {
        return MercuryoSellRequestInterceptor(okHttpClient, gson, tradeSellCallback)
    }
}

class MercuryoBuyRequestInterceptor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val callback: TradeBuyCallback
) : WebViewRequestInterceptor {

    private val interceptionPattern = Regex("https://api\\.mercuryo\\.io/[a-zA-Z0-9.]+/widget/buy/([a-zA-Z0-9]+)/status.*")

    override fun intercept(request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        val matches = interceptionPattern.find(url)

        if (matches != null) {
            val orderId = matches.groupValues[1]
            return performOkHttpRequest(orderId, request)
        }

        return false
    }

    private fun performOkHttpRequest(orderId: String, request: WebResourceRequest): Boolean {
        val requestBuilder = request.toOkHttpRequestBuilder()

        return try {
            val response = okHttpClient.makeRequestBlocking(requestBuilder)
            val buyStatusResponse = gson.fromJson(response.body!!.string(), BuyStatusResponse::class.java)

            if (buyStatusResponse.isPaid()) {
                callback.onBuyCompleted(orderId)
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * https://api.mercuryo.io/v1.6/widget/buy/0da637056a0c85319/status?user_deviceid=5add1aa846969ead8e1f0d5d4da43d65&user_user_agent=Mozilla%2F5.0+(Linux%3B+Android+11%3B+RMX2063+Build%2FRKQ1.201112.002%3B+wv)+AppleWebKit%2F537.36+(KHTML,+like+Gecko)+Version%2F4.0+Chrome%2F134.0.6998.135+Mobile+Safari%2F537.36&user_time_zone=-120&user_language=ru&user_platform=Linux+aarch64
 * {
 *     "status": 200,
 *     "data": {
 *         "id": "0da637056a0c85319",
 *         "status": "paid", // new, pending, paid
 *         "payment_status": "charged",
 *         "withdraw_transaction": {
 *             "id": "0da63727a3ce33010",
 *             "address": "12gkMmfdKq7aEnAXwb2NSxh9vLqKifoCaoafLrR6E6swZRmc",
 *             "fee": "0",
 *             "url": ""
 *         },
 *         "currency": "DOT",
 *         "amount": "2.4742695641",
 *         "fiat_currency": "USD",
 *         "fiat_amount": "11.00",
 *         "address": "12gkMmfdKq7aEnAXwb2NSxh9vLqKifoCaoafLrR6E6swZRmc",
 *         "transaction": {
 *             "id": "0da637056f0821757"
 *         },
 *         "local_fiat_currency_total": {
 *             "local_fiat_currency": "EUR",
 *             "local_fiat_amount": "10.25"
 *         }
 *     }
 * }
 */
private class BuyStatusResponse(val data: Data) {

    class Data(val status: String)
}

private fun BuyStatusResponse.isPaid() = data.status == "paid"
