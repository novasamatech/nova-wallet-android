package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.referendum

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.referendum.request.ReferendumDetailsV2Request
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.referendum.request.ReferendumPreviewV2Request
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.referendum.response.ReferendaPreviewV2Response
import io.novafoundation.nova.feature_governance_impl.data.offchain.v2.referendum.response.ReferendumDetailsV2Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface PolkassemblyV2Api {

    @POST
    suspend fun getReferendumPreviews(
        @Url url: String,
        @Body body: ReferendumPreviewV2Request
    ): SubQueryResponse<ReferendaPreviewV2Response>

    @POST
    suspend fun getReferendumDetails(
        @Url url: String,
        @Body body: ReferendumDetailsV2Request
    ): SubQueryResponse<ReferendumDetailsV2Response>
}
