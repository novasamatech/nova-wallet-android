package io.novafoundation.nova.feature_dapp_impl.utils.integrityCheck

import retrofit2.http.Body
import retrofit2.http.POST

interface IntegrityCheckApi {

    @POST("challenges")
    suspend fun getChallenge(): ChallengeResponse

    @POST("app-integrity/attestations")
    suspend fun attest(@Body request: AttestRequest)
}

class ChallengeResponse(val challenge: String)

class AttestRequest(
    val appIntegrityId: String,
    val publicKey: String,
    val integrityToken: String,
    val challenge: String
)
