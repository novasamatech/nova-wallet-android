package io.novafoundation.nova.feature_dapp_impl.data.network.competitors

import retrofit2.http.GET
import retrofit2.http.Url

interface StakingCompetitorDomainsApi {

    @GET
    suspend fun getDomains(@Url url: String): StakingCompetitorsResponse
}
