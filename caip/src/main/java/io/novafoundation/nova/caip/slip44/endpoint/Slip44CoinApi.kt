package io.novafoundation.nova.caip.slip44.endpoint

import retrofit2.http.GET
import retrofit2.http.Url

interface Slip44CoinApi {

    @GET
    suspend fun getSlip44Coins(@Url url: String): List<Slip44CoinRemote>
}
