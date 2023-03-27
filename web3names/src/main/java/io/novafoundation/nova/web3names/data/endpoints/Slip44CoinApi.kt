package io.novafoundation.nova.web3names.data.endpoints

import io.novafoundation.nova.web3names.data.endpoints.model.Slip44CoinRemote
import retrofit2.http.GET
import retrofit2.http.Url

interface Slip44CoinApi {

    @GET
    suspend fun getSlip44Coins(@Url() url: String): List<Slip44CoinRemote>
}
