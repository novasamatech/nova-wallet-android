package io.novafoundation.nova.feature_account_impl.data.proxy.network.api

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_account_impl.data.proxy.network.api.request.FindProxiesRequest
import io.novafoundation.nova.feature_account_impl.data.proxy.network.api.response.FindProxiesResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface FindProxiesApi {
    @POST
    suspend fun findProxies(@Url url: String, @Body body: FindProxiesRequest): SubQueryResponse<FindProxiesResponse>
}
