package io.novafoundation.nova.infrastructure.attestation

import androidx.annotation.Keep
import retrofit2.http.Body
import retrofit2.http.POST

@Keep
class AttestationChallengeResponse(val challenge: String)

@Keep
class AttestationRegisterRequest(
    val client_id: String,
    val public_key: String,
    val platform: String,
    val app_package: String,
    val attestation_type: String,
    val challenge: String,
    val integrity_token: String?
)

interface AttestationApi {

    @POST("v1/attestation/challenges")
    suspend fun challenge(): AttestationChallengeResponse

    @POST("v1/attestation/register")
    suspend fun register(@Body request: AttestationRegisterRequest)
}
