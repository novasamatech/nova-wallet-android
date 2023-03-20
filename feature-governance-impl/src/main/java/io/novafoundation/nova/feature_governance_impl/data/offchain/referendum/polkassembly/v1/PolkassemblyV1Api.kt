package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.request.ParachainReferendumDetailsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.request.ReferendumDetailsRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.request.ParachainReferendumPreviewRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.request.ReferendumPreviewRequest
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.response.ParachainReferendaPreviewResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.response.ReferendaPreviewResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.response.ReferendumDetailsResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface PolkassemblyV1Api {

    @POST
    suspend fun getReferendumPreviews(
        @Url url: String,
        @Body body: ReferendumPreviewRequest
    ): SubQueryResponse<ReferendaPreviewResponse>

    @POST
    suspend fun getParachainReferendumPreviews(
        @Url url: String,
        @Body body: ParachainReferendumPreviewRequest
    ): SubQueryResponse<ParachainReferendaPreviewResponse>

    @POST
    suspend fun getReferendumDetails(
        @Url url: String,
        @Body body: ReferendumDetailsRequest
    ): SubQueryResponse<ReferendumDetailsResponse>

    @POST
    suspend fun getParachainReferendumDetails(
        @Url url: String,
        @Body body: ParachainReferendumDetailsRequest
    ): SubQueryResponse<ReferendumDetailsResponse>
}
