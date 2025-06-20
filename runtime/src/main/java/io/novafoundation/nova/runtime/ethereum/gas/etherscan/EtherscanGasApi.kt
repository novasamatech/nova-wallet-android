package io.novafoundation.nova.runtime.ethereum.gas.etherscan

import io.novafoundation.nova.common.data.network.UserAgent
import io.novafoundation.nova.runtime.network.etherscan.model.EtherscanResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url

interface EtherscanGasApi {

    @GET
    @Headers(UserAgent.NOVA)
    suspend fun getGasEstimate(
        @Url baseUrl: String,
        @Query("apikey") apiKey: String?,
        @Query("module") module: String = "gastracker",
        @Query("action") action: String = "gasoracle",
    ): EtherscanResponse<EtherscanGasResponse>
}
