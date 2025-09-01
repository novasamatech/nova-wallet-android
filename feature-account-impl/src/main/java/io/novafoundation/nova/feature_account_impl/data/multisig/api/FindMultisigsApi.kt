package io.novafoundation.nova.feature_account_impl.data.multisig.api

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_account_impl.BuildConfig
import io.novafoundation.nova.feature_account_impl.data.multisig.api.request.FindMultisigsRequest
import io.novafoundation.nova.feature_account_impl.data.multisig.api.request.OffChainPendingMultisigInfoRequest
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.FindMultisigsResponse
import io.novafoundation.nova.feature_account_impl.data.multisig.api.response.GetPedingMultisigOperationsResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface FindMultisigsApi {

    companion object {

        const val MULTICHAIN_SYNC_URL = BuildConfig.EXTERNAL_ACCOUNTS_SYNC_URL
    }

    @POST(MULTICHAIN_SYNC_URL)
    suspend fun findMultisigs(
        @Body body: FindMultisigsRequest
    ): SubQueryResponse<FindMultisigsResponse>

    @POST
    suspend fun getCallDatas(
        @Url url: String,
        @Body body: OffChainPendingMultisigInfoRequest
    ): SubQueryResponse<GetPedingMultisigOperationsResponse>
}
