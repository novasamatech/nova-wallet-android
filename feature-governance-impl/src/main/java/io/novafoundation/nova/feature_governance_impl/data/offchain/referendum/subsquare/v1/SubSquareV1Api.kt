package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1

import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1.response.ReferendaPreviewV1Response
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1.response.ReferendumDetailsV1Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface SubSquareV1Api {

    @GET
    suspend fun getReferendumPreviews(
        @Url url: String,
        @Query("page_size") pageSize: Int = 1000
    ): ReferendaPreviewV1Response

    @GET
    suspend fun getReferendumDetails(@Url url: String): ReferendumDetailsV1Response
}
