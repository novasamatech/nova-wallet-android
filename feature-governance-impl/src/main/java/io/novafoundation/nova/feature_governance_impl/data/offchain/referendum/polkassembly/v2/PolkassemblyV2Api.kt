package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.request.ReferendumDetailsV2Request
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.request.ReferendumPreviewV2Request
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.response.ReferendaPreviewV2Response
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.response.ReferendumDetailsV2Response
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
