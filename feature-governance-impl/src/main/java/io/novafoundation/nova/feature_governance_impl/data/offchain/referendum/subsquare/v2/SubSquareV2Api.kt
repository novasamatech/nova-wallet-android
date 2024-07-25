package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2

import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2.response.ReferendaPreviewV2Response
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2.response.ReferendumDetailsV2Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface SubSquareV2Api {

    @GET
    suspend fun getReferendumPreviews(
        @Url url: String,
        @Query("page_size") pageSize: Int = 100
    ): ReferendaPreviewV2Response

    @GET
    suspend fun getReferendumDetails(@Url url: String): ReferendumDetailsV2Response
}
