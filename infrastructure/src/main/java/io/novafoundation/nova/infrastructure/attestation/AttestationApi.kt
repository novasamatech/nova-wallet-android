package io.novafoundation.nova.infrastructure.attestation

import androidx.annotation.Keep
import retrofit2.http.Body
import retrofit2.http.POST

const val CHALLENGES_PATH = "v1/attestation/challenges"
const val REGISTER_PATH = "v1/attestation/register"

/** The backend rejects unknown members, so these carry exactly the contract's fields and nothing else. */
@Keep
class AttestationChallengeRequest(
    val client_id: String,
    val purpose: String,
    val profile: Int
)

@Keep
class AttestationChallengeResponse(val challenge: String)

@Keep
class AttestationRegisterRequest(
    val profile: Int,
    val client_id: String,
    val challenge: String,
    val platform: String,
    val app_id: String,
    val attestation_type: String,
    val key_reference: String,
    val app_attest_environment: String,
    val integrity_token: String,
    val signature: String
)

interface AttestationApi {

    @POST(CHALLENGES_PATH)
    suspend fun challenge(@Body request: AttestationChallengeRequest): AttestationChallengeResponse

    @POST(REGISTER_PATH)
    suspend fun register(@Body request: AttestationRegisterRequest)
}
