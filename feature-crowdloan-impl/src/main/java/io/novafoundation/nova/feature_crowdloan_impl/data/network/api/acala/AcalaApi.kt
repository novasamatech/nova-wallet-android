package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala

import io.novafoundation.nova.feature_crowdloan_impl.BuildConfig
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.requireGenesisHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

private fun authHeader(token: String) = "Bearer $token"

interface AcalaApi {

    companion object {
        private val URL_BY_GENESIS = mapOf(
            Chain.Geneses.ROCOCO_ACALA to "crowdloan.aca-dev.network",
            Chain.Geneses.POLKADOT to "crowdloan.aca-api.network",
            Chain.Geneses.KUSAMA to "api.aca-staging.network"
        )

        private val AUTH_BY_GENESIS = mapOf(
            Chain.Geneses.POLKADOT to authHeader(BuildConfig.ACALA_PROD_AUTH_TOKEN),
            Chain.Geneses.ROCOCO_ACALA to authHeader(BuildConfig.ACALA_TEST_AUTH_TOKEN)
        )

        fun getAuthHeader(chain: Chain) = AUTH_BY_GENESIS[chain.requireGenesisHash()]
            ?: notSupportedChain(chain)

        fun getBaseUrl(chain: Chain) = URL_BY_GENESIS[chain.requireGenesisHash()]
            ?: notSupportedChain(chain)

        private fun notSupportedChain(chain: Chain): Nothing {
            throw UnsupportedOperationException("Chain ${chain.name} is not supported for Acala/Karura crowdloans")
        }
    }

    @GET("//{baseUrl}/referral/{referral}")
    suspend fun isReferralValid(
        @Header("Authorization") authHeader: String,
        @Path("baseUrl") baseUrl: String,
        @Path("referral") referral: String,
    ): ReferralCheck

    @GET("//{baseUrl}/statement")
    suspend fun getStatement(
        @Header("Authorization") authHeader: String,
        @Path("baseUrl") baseUrl: String,
    ): AcalaStatement

    @POST("//{baseUrl}/contribute")
    suspend fun directContribute(
        @Header("Authorization") authHeader: String,
        @Path("baseUrl") baseUrl: String,
        @Body body: AcalaDirectContributeRequest,
    ): Any?

    @POST("//{baseUrl}/transfer")
    suspend fun liquidContribute(
        @Header("Authorization") authHeader: String,
        @Path("baseUrl") baseUrl: String,
        @Body body: AcalaLiquidContributeRequest,
    ): Any?

    @GET("//{baseUrl}/contribution/{address}")
    suspend fun getContributions(
        @Path("baseUrl") baseUrl: String,
        @Path("address") address: String,
    ): AcalaContribution
}

suspend fun AcalaApi.getContributions(chain: Chain, accountId: AccountId) = getContributions(AcalaApi.getBaseUrl(chain), chain.addressOf(accountId))
