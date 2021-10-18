package jp.co.soramitsu.feature_crowdloan_impl.data.network.api.karura

import jp.co.soramitsu.runtime.ext.Geneses
import jp.co.soramitsu.runtime.ext.genesisHash
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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
    suspend fun isReferralValid(
        @Path("baseUrl") baseUrl: String,
        @Path("referral") referral: String,
    ): ReferralCheck

    @GET("//{baseUrl}/statement")
    suspend fun getStatement(
        @Path("baseUrl") baseUrl: String,
    ): AcalaStatement

    @POST("//{baseUrl}/verify")
    suspend fun applyForBonus(
        @Path("baseUrl") baseUrl: String,
        @Body body: VerifyKaruraParticipationRequest,
    ): Any?
}
