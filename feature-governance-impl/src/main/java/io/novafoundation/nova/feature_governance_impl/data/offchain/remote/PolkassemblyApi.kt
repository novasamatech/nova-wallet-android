package io.novafoundation.nova.feature_governance_impl.data.offchain.remote

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.request.ReferendumDetailsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.request.ReferendumPreviewRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.response.ReferendaPreviewResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.remote.model.response.ReferendumDetailsResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface PolkassemblyApi {

    @POST
    suspend fun getReferendumPreviews(
        @Url url: String,
        @Body body: ReferendumPreviewRequest
    ): SubQueryResponse<ReferendaPreviewResponse>

    @POST
    suspend fun getReferendumDetails(
        @Url url: String,
        @Body body: ReferendumDetailsRequest
    ): SubQueryResponse<ReferendumDetailsResponse>
}
