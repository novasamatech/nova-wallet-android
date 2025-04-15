package io.novafoundation.nova.feature_account_impl.data.multisig.api

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_account_impl.data.multisig.api.request.FindMultisigsRequest
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.FindMultisigsResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface FindMultisigsApi {

    @POST
    suspend fun findMultisigs(
        @Url url: String,
        @Body body: FindMultisigsRequest
    ): SubQueryResponse<FindMultisigsResponse>
}
