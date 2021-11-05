package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.bifrost

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BifrostApi {

    companion object {
        const val BASE_URL = "https://salp-api.bifrost.finance"
    }

    @POST("/")
    suspend fun getAccountByReferralCode(@Body body: BifrostReferralCheckRequest): SubQueryResponse<GetAccountByReferralCodeResponse>
}

suspend fun BifrostApi.getAccountByReferralCode(code: String) = getAccountByReferralCode(BifrostReferralCheckRequest(code))
