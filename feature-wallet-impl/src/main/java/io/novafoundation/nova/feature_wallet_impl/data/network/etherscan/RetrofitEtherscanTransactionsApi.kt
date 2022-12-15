package io.novafoundation.nova.feature_wallet_impl.data.network.etherscan

import io.novafoundation.nova.common.data.network.UserAgent
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.EtherscanAccountTransfer
import io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model.EtherscanResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url

interface RetrofitEtherscanTransactionsApi {

    @GET
    @Headers(UserAgent.NOVA)
    suspend fun getOperationsHistory(
        @Url baseUrl: String,
        @Query("contractaddress") contractAddress: String,
        @Query("address") accountAddress: String,
        @Query("page") pageNumber: Int,
        @Query("offset") pageSize: Int,
        @Query("apikey") apiKey: String?,
        @Query("module") module: String = "account",
        @Query("action") action: String = "tokentx",
        @Query("sort") sort: String = "desc"
    ): EtherscanResponse<List<EtherscanAccountTransfer>>
}
