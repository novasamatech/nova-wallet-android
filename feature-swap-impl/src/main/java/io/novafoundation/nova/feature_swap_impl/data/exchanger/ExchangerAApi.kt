package io.novafoundation.nova.feature_swap_impl.data.exchanger

import okhttp3.HttpUrl
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

/** Exchanger A integration through the Nova backend. */
interface ExchangerAApi {

    @GET
    suspend fun assets(@Url url: HttpUrl): ExchangerAAssetsRemote

    @POST
    suspend fun quote(@Url url: HttpUrl, @Body request: ExchangerAQuoteRequest): ExchangerAQuoteRemote
}
