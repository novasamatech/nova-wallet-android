package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parallel

import retrofit2.http.GET
import retrofit2.http.Path

interface ParallelApi {

    companion object {
        const val BASE_URL = "https://auction-service-prod.parallel.fi/crowdloan/rewards/"
    }

    @GET("{network}/{address}")
    suspend fun getContributions(
        @Path("network") network: String,
        @Path("address") address: String,
    ): List<ParallelContribution>
}
