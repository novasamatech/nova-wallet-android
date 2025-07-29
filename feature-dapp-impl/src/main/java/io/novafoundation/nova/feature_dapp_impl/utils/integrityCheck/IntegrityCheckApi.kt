package io.novafoundation.nova.feature_dapp_impl.utils.integrityCheck

import retrofit2.http.POST

interface IntegrityCheckApi {

    @POST("/auth/challenges")
    suspend fun getChallenge(): ChallengeResponse

    @POST("/auth/app-integrity/attestations")
    suspend fun attest(request: AttestRequest)
}

class ChallengeResponse(val challenge: String)


class AttestRequest(
    val appIntegrityId: String,
    val publicKey: String,
    val integrityToken: String,
    val challenge: String
)

