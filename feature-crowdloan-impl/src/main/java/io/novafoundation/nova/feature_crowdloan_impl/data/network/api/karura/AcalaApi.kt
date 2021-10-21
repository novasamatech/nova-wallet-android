package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.karura

import io.novafoundation.nova.feature_crowdloan_impl.BuildConfig
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

private const val AUTH_HEADER = "Authorization: Bearer ${BuildConfig.ACALA_AUTH_TOKEN}"

interface AcalaApi {

    companion object {
        private val URL_BY_GENESIS = mapOf(
            Chain.Geneses.ROCOCO_ACALA to "crowdloan.aca-dev.network",
            Chain.Geneses.KUSAMA to "api.aca-staging.network"
        )

        fun getBaseUrl(chain: Chain) = URL_BY_GENESIS[chain.genesisHash]
            ?: throw UnsupportedOperationException("Chain ${chain.name} is not supported for Acala/Karura crowdloans")
    }

    @GET("//{baseUrl}/referral/{referral}")
    @Headers(AUTH_HEADER)
    suspend fun isReferralValid(
        @Path("baseUrl") baseUrl: String,
        @Path("referral") referral: String,
    ): ReferralCheck

    @GET("//{baseUrl}/statement")
    @Headers(AUTH_HEADER)
    suspend fun getStatement(
        @Path("baseUrl") baseUrl: String,
    ): AcalaStatement

    @POST("//{baseUrl}/contribute")
    @Headers(AUTH_HEADER)
    suspend fun applyForBonus(
        @Path("baseUrl") baseUrl: String,
        @Body body: VerifyKaruraParticipationRequest,
    ): Any?
}
