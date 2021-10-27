package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam

import io.novafoundation.nova.common.data.network.TimeHeaderInterceptor
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_api.data.repository.getExtra
import io.novafoundation.nova.feature_crowdloan_impl.BuildConfig
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

private val AUTH_TOKENS = mapOf(
    "MOONBEAM_TEST_AUTH_TOKEN" to BuildConfig.MOONBEAM_TEST_AUTH_TOKEN,
    "MOONBEAM_PROD_AUTH_TOKEN" to BuildConfig.MOONBEAM_PROD_AUTH_TOKEN
)

interface MoonbeamApi {

    @GET("https://raw.githubusercontent.com/moonbeam-foundation/crowdloan-self-attestation/main/moonbeam/README.md")
    suspend fun getLegalText(): String

    @GET("//{baseUrl}/check-remark/{address}")
    suspend fun checkRemark(
        @Path("baseUrl") baseUrl: String,
        @Header("x-api-key") apiToken: String?,
        @Path("address") address: String,
    ): CheckRemarkResponse

    @POST("//{baseUrl}/agree-remark")
    suspend fun agreeRemark(
        @Path("baseUrl") baseUrl: String,
        @Header("x-api-key") apiToken: String?,
        @Body body: AgreeRemarkRequest,
    ): AgreeRemarkResponse

    @POST("//{baseUrl}/verify-remark")
    @Headers(TimeHeaderInterceptor.LONG_CONNECT, TimeHeaderInterceptor.LONG_READ, TimeHeaderInterceptor.LONG_WRITE)
    suspend fun verifyRemark(
        @Path("baseUrl") baseUrl: String,
        @Header("x-api-key") apiToken: String?,
        @Body body: VerifyRemarkRequest,
    ): VerifyRemarkResponse

    @POST("//{baseUrl}/make-signature")
    suspend fun makeSignature(
        @Path("baseUrl") baseUrl: String,
        @Header("x-api-key") apiToken: String?,
        @Body body: MakeSignatureRequest,
    ): MakeSignatureResponse
}

fun ParachainMetadata.moonbeamChainId() = getExtra("paraId")

private fun ParachainMetadata.apiBaseUrl() = getExtra("apiLink")
private fun ParachainMetadata.apiToken() = AUTH_TOKENS[getExtra("apiTokenName")]

suspend fun MoonbeamApi.checkRemark(chainMetadata: ParachainMetadata, address: String): CheckRemarkResponse {
    return checkRemark(chainMetadata.apiBaseUrl(), chainMetadata.apiToken(), address)
}

suspend fun MoonbeamApi.agreeRemark(chainMetadata: ParachainMetadata, body: AgreeRemarkRequest): AgreeRemarkResponse {
    return agreeRemark(chainMetadata.apiBaseUrl(), chainMetadata.apiToken(), body)
}

suspend fun MoonbeamApi.verifyRemark(chainMetadata: ParachainMetadata, body: VerifyRemarkRequest): VerifyRemarkResponse {
    return verifyRemark(chainMetadata.apiBaseUrl(), chainMetadata.apiToken(), body)
}

suspend fun MoonbeamApi.makeSignature(chainMetadata: ParachainMetadata, body: MakeSignatureRequest): MakeSignatureResponse {
    return makeSignature(chainMetadata.apiBaseUrl(), chainMetadata.apiToken(), body)
}
