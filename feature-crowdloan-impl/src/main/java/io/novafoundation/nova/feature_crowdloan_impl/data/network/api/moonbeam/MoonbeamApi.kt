package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam

import io.novafoundation.nova.common.data.network.TimeHeaderInterceptor
import io.novafoundation.nova.feature_crowdloan_impl.BuildConfig
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

private const val AUTH_HEADER = "x-api-key: ${BuildConfig.MOONBEAM_AUTH_TOKEN}"

private val URL_BY_GENESES = mapOf(
    Chain.Geneses.POLKATRAIN to "wallet-test.api.purestake.xyz"
)

private fun urlOf(chainId: ChainId) = URL_BY_GENESES[chainId] ?: throw IllegalArgumentException("Cannot find url for $chainId to create Moonbeam api")

interface MoonbeamApi {

    @GET("https://raw.githubusercontent.com/moonbeam-foundation/crowdloan-self-attestation/main/moonbeam/README.md")
    suspend fun getLegalText(): String

    @GET("//{baseUrl}/check-remark/{address}")
    @Headers(AUTH_HEADER)
    suspend fun checkRemark(
        @Path("baseUrl") baseUrl: String,
        @Path("address") address: String,
    ): CheckRemarkResponse

    @POST("//{baseUrl}/agree-remark")
    @Headers(AUTH_HEADER)
    suspend fun agreeRemark(
        @Path("baseUrl") baseUrl: String,
        @Body body: AgreeRemarkRequest,
    ): AgreeRemarkResponse

    @POST("//{baseUrl}/verify-remark")
    @Headers(AUTH_HEADER, TimeHeaderInterceptor.LONG_CONNECT, TimeHeaderInterceptor.LONG_READ, TimeHeaderInterceptor.LONG_WRITE)
    suspend fun verifyRemark(
        @Path("baseUrl") baseUrl: String,
        @Body body: VerifyRemarkRequest,
    ): VerifyRemarkResponse
}

suspend fun MoonbeamApi.checkRemark(chain: Chain, address: String) = checkRemark(urlOf(chain.id), address)

suspend fun MoonbeamApi.agreeRemark(chain: Chain, body: AgreeRemarkRequest) = agreeRemark(urlOf(chain.id), body)

suspend fun MoonbeamApi.verifyRemark(chain: Chain, body: VerifyRemarkRequest) = verifyRemark(urlOf(chain.id), body)
