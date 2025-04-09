package io.novafoundation.nova.feature_buy_impl.presentation.trade.interceptors.mercuryo

import android.webkit.WebResourceRequest
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.webView.makeRequestBlocking
import io.novafoundation.nova.common.utils.webView.toOkHttpRequestBuilder
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoBuyRequestInterceptor
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoBuyRequestInterceptorFactory
import okhttp3.OkHttpClient

class RealMercuryoBuyRequestInterceptorFactory(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : MercuryoBuyRequestInterceptorFactory {
    override fun create(onTradeOperationFinishedListener: OnTradeOperationFinishedListener): MercuryoBuyRequestInterceptor {
        return RealMercuryoBuyRequestInterceptor(okHttpClient, gson, onTradeOperationFinishedListener)
    }
}

class RealMercuryoBuyRequestInterceptor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val onTradeOperationFinishedListener: OnTradeOperationFinishedListener
) : MercuryoBuyRequestInterceptor {

    private val interceptionPattern = Regex("https://api\\.mercuryo\\.io/[a-zA-Z0-9.]+/widget/buy/([a-zA-Z0-9]+)/status.*")

    override fun intercept(request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        val matches = interceptionPattern.find(url)

        if (matches != null) {
            return performOkHttpRequest(request)
        }

        return false
    }

    private fun performOkHttpRequest(request: WebResourceRequest): Boolean {
        val requestBuilder = request.toOkHttpRequestBuilder()

        return try {
            val response = okHttpClient.makeRequestBlocking(requestBuilder)
            val buyStatusResponse = gson.fromJson(response.body!!.string(), BuyStatusResponse::class.java)

            if (buyStatusResponse.isPaid()) {
                onTradeOperationFinishedListener.onTradeOperationFinished(success = true)
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
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
