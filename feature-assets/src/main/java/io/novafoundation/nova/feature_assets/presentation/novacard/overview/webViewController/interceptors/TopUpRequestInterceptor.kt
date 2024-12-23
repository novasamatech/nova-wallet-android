package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors

import android.webkit.WebResourceRequest
import com.google.gson.Gson
import java.math.BigDecimal
import okhttp3.OkHttpClient

class TopUpRequestInterceptorFactory(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    fun create(callback: TopUpRequestInterceptor.Callback): TopUpRequestInterceptor {
        return TopUpRequestInterceptor(okHttpClient, gson, callback)
    }
}

class TopUpRequestInterceptor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val onCardCreatedListener: Callback
) : NovaCardInterceptor {

    interface Callback {

        fun onCardTopUpCompleted(orderId: String)

        fun onTopUpStart(orderId: String, amount: BigDecimal, address: String)
    }

    override fun intercept(request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        val pattern = Regex("https://api\\.mercuryo\\.io/[a-zA-Z0-9.]+/widget/sell-request/([a-zA-Z0-9]+)/status.*")

        val matches = pattern.find(url)

        if (matches != null) {
            val orderId = matches.groupValues[1]
            return performOkHttpRequest(orderId, request)
        }

        return false
    }

    private fun performOkHttpRequest(orderId: String, request: WebResourceRequest): Boolean {
        val requestBuilder = request.toOkHttpRequestBuilder()

        return try {
            val response = makeRequest(okHttpClient, requestBuilder)
            val topUpResponse = gson.fromJson(response.body!!.string(), TopUpResponse::class.java)

            when {
                topUpResponse.isNew() -> onCardCreatedListener.onTopUpStart(orderId, topUpResponse.getAmount(), topUpResponse.getAddress())

                topUpResponse.isCompleted() -> onCardCreatedListener.onCardTopUpCompleted(orderId)
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
private class TopUpResponse(val data: Data) {

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

private fun TopUpResponse.getAmount(): BigDecimal {
    return data.amounts.request.amount.toBigDecimal()
}

private fun TopUpResponse.getAddress(): String {
    return data.address
}

private fun TopUpResponse.isNew() = data.status == "new"

private fun TopUpResponse.isCompleted() = data.status == "completed"
