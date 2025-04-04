package io.novafoundation.nova.feature_buy_impl.presentation.trade.interceptors.mercuryo

import android.webkit.WebResourceRequest
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.webView.makeRequestBlocking
import io.novafoundation.nova.common.utils.webView.toOkHttpRequestBuilder
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnSellOrderCreatedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.common.OnTradeOperationFinishedListener
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoSellRequestInterceptor
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoSellRequestInterceptorFactory
import java.math.BigDecimal
import okhttp3.OkHttpClient

class RealMercuryoSellRequestInterceptorFactory(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : MercuryoSellRequestInterceptorFactory {
    override fun create(
        tradeSellCallback: OnSellOrderCreatedListener,
        onTradeOperationFinishedListener: OnTradeOperationFinishedListener
    ): MercuryoSellRequestInterceptor {
        return RealMercuryoSellRequestInterceptor(okHttpClient, gson, tradeSellCallback, onTradeOperationFinishedListener)
    }
}

class RealMercuryoSellRequestInterceptor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val tradeSellCallback: OnSellOrderCreatedListener,
    private val onTradeOperationFinishedListener: OnTradeOperationFinishedListener
) : MercuryoSellRequestInterceptor {

    private val openedOrderIds = mutableSetOf<String>()

    private val interceptionPattern = Regex("https://api\\.mercuryo\\.io/[a-zA-Z0-9.]+/widget/sell-request/([a-zA-Z0-9]+)/status.*")

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
            val sellStatusResponse = gson.fromJson(response.body!!.string(), SellStatusResponse::class.java)

            when {
                sellStatusResponse.isNew() && orderId !in openedOrderIds -> {
                    openedOrderIds.add(orderId)
                    tradeSellCallback.onSellOrderCreated(orderId, sellStatusResponse.getAddress(), sellStatusResponse.getAmount())
                }

                sellStatusResponse.isCompleted() -> onTradeOperationFinishedListener.onTradeOperationFinished()
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
 *         "status": "completed", // Status may be new, pending, completed
 *         "is_partially_paid": 0,
 *         "amounts": {
 *             "request": {
 *                 "amount": "3.55",
 *                 "currency": "DOT",
 *                 "fiat_amount": "25.00",
 *                 "fiat_currency": "EUR"
 *             },
 *             "deposit": {
 *                 "amount": "3.5544722879",
 *                 "currency": "DOT",
 *                 "fiat_amount": "28.00",
 *                 "fiat_currency": "EUR"
 *             },
 *             "payout": {
 *                 "amount": "3.5544722879",
 *                 "currency": "DOT",
 *                 "fiat_amount": "24.98",
 *                 "fiat_currency": "EUR"
 *             }
 *         },
 *         "next": null,
 *         "deposit_transaction": {
 *             "id": "1gb8dnc28jds8ch",
 *             "address": "15AsDPtQ6rZdJgsLsEmQCahym5STRVBVaUYjWFiRRinMjYYaw",
 *             "url": "https://polkadot.subscan.io/extrinsic/0x178f96e1f8837a3dd75ff8b5a5d4422c5c0f7848fbf5c00e343f03b9466e408b"
 *         },
 *         "address": "15AsDPtQ6rZdJgsLsEmQCahym5STRVBVaUYjWFiRRinMjYYaw",
 *         "fiat_card_id": "1gb8dnc28jds8ch"
 *     }
 * }
 */
private class SellStatusResponse(val data: Data) {

    class Data(
        val status: String,
        val amounts: Amounts,
        val address: String
    )

    class Amounts(val request: Request) {

        class Request(
            val amount: String,
        )
    }
}

private fun SellStatusResponse.getAmount(): BigDecimal {
    return data.amounts.request.amount.toBigDecimal()
}

private fun SellStatusResponse.getAddress(): String {
    return data.address
}

private fun SellStatusResponse.isNew() = data.status == "new"

private fun SellStatusResponse.isCompleted() = data.status == "completed"
