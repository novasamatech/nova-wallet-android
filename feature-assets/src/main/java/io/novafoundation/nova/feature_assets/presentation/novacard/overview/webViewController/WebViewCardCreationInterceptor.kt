package io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.onEachLatest
import io.novafoundation.nova.common.utils.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.time.Duration.Companion.milliseconds

interface OnCardCreatedListener {

    fun onCardCreated()
}

class WebViewCardCreationInterceptorFactory(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    fun create(onCardCreatedListener: OnCardCreatedListener): WebViewCardCreationInterceptor {
        return WebViewCardCreationInterceptor(okHttpClient, gson, onCardCreatedListener)
    }
}

class WebViewCardCreationInterceptor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val onCardCreatedListener: OnCardCreatedListener
) {

    private var interceptedCardRequest = MutableStateFlow<Request.Builder?>(null)

    fun runPolling(coroutineScope: CoroutineScope) {
        interceptedCardRequest.onEachLatest { request ->
            if (request == null) return@onEachLatest

            for (i in 0..100) {
                val cardWasSuccessfullyCreated = makeRequest(request)

                if (cardWasSuccessfullyCreated) {
                    break
                }

                delay(1000.milliseconds)
            }
        }.inBackground()
            .launchIn(coroutineScope)
    }

    /**
     * @return true if the request was intercepted otherwise false
     */
    fun intercept(request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        // Intercept requests that might return a JSON response
        if (url.contains("https://api.mercuryo.io/v1.6/cards")) { // Specify your condition here
            performOkHttpRequest(request)
            return true
        }

        return false
    }

    private fun performOkHttpRequest(request: WebResourceRequest) {
        try {
            // Create OkHttp Request based on WebResourceRequest
            val okHttpRequestBuilder = Request.Builder().url(request.url.toString())

            // Set method (GET, POST, etc.) and request body if needed
            when (request.method) {
                "GET" -> okHttpRequestBuilder.get()
                else -> okHttpRequestBuilder.get()
            }

            // Add headers from WebResourceRequest
            for ((key, value) in request.requestHeaders) {
                okHttpRequestBuilder.addHeader(key, value)
            }

            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie(request.url.toString())
            if (cookies != null) {
                okHttpRequestBuilder.addHeader("Cookie", cookies)
            }

            interceptedCardRequest.value = okHttpRequestBuilder

            makeRequest(okHttpRequestBuilder)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Notify onCardCreatedListener if data value contains mercurio cards
     */
    private fun makeRequest(requestBuilder: Request.Builder): Boolean {
        // Execute the OkHttp request
        val okHttpResponse = okHttpClient.newCall(requestBuilder.build()).execute()

        // Check if the response is successful
        if (okHttpResponse.isSuccessful) {
            val responseBody = okHttpResponse.body

            val data = responseBody!!.byteStream().readText()
            val cardsResponse = gson.fromJson<CardsResponse>(data)
            val cards = cardsResponse.data
            val containsMercurioCard = cards.any { it.issuedByMercurio }

            if (containsMercurioCard) {
                onCardCreatedListener.onCardCreated()
                return true
            }
        }

        return false
    }
}

/**
 * The full response is: {"status":200,"total":1,"next":null,"prev":null,"data":[{"id":"0c82dafa12d352193","created_at":"2024-08-23 10:12:10","payment_system":"Mastercard","card_number":"************7907","card_expiration_month":"08","card_expiration_year":"2029","bank":null,"issued_by_mercuryo":true,"fiat_card_id":"0c82dab8d9d316059","fiat_card_status":"active"}]}
 */
private class CardsResponse(val data: List<Card>) {

    class Card(@SerializedName("issued_by_mercuryo") val issuedByMercurio: Boolean)
}
