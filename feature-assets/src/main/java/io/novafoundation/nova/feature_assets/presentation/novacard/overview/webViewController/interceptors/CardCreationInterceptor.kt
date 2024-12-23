package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient

class CardCreationInterceptorFactory(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    fun create(callback: CardCreationInterceptor.Callback): CardCreationInterceptor {
        return CardCreationInterceptor(okHttpClient, gson, callback)
    }
}

class CardCreationInterceptor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val onCardCreatedListener: Callback
) : NovaCardInterceptor {

    interface Callback {

        fun onCardCreated()
    }

    /**
     * @return true if the request was intercepted otherwise false
     */
    override fun intercept(request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        if (url.contains("https://api.mercuryo.io/v1.6/cards")) { // Specify your condition here
            return performOkHttpRequest(request)
        }

        return false
    }

    private fun performOkHttpRequest(request: WebResourceRequest): Boolean {
        val requestBuilder = request.toOkHttpRequestBuilder()

        return try {
            val response = makeRequest(okHttpClient, requestBuilder)
            val cardsResponse = gson.fromJson(response.body!!.string(), CardsResponse::class.java)

            if (cardsResponse.isCardCreated()) {
                onCardCreatedListener.onCardCreated()
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * The full response is: {"status":200,"total":1,"next":null,"prev":null,"data":[{"id":"0c82dafa12d352193","created_at":"2024-08-23 10:12:10","payment_system":"Mastercard","card_number":"************7907","card_expiration_month":"08","card_expiration_year":"2029","bank":null,"issued_by_mercuryo":true,"fiat_card_id":"0c82dab8d9d316059","fiat_card_status":"active"}]}
 */
private class CardsResponse(val data: List<Card>) {

    class Card(@SerializedName("issued_by_mercuryo") val issuedByMercurio: Boolean)
}

private fun CardsResponse.isCardCreated() = data.any { it.issuedByMercurio }
