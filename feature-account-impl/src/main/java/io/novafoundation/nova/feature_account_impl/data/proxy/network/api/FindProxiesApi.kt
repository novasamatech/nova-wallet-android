package io.novafoundation.nova.feature_account_impl.data.proxy.network.api

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_account_impl.BuildConfig
import io.novafoundation.nova.feature_account_impl.data.proxy.network.api.request.FindProxiesRequest
import io.novafoundation.nova.feature_account_impl.data.proxy.network.api.response.FindProxiesResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface FindProxiesApi {

    companion object {

        const val PROXY_SYNC_URL = BuildConfig.EXTERNAL_ACCOUNTS_SYNC_URL
    }


    @POST(PROXY_SYNC_URL)
    suspend fun findProxies(@Body body: FindProxiesRequest): SubQueryResponse<FindProxiesResponse>
}
