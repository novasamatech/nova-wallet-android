package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parallel

import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
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

private val Chain.networkPath
    get() = name.toLowerCase()

suspend fun ParallelApi.getContributions(chain: Chain, accountId: AccountId) = getContributions(
    network = chain.networkPath,
    address = chain.addressOf(accountId)
)
