package io.novafoundation.nova.infrastructure.attestation

import androidx.annotation.Keep
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

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

    // Full URLs: the host comes from the global config at request time, see InfrastructureUrls
    @POST
    suspend fun challenge(@Url url: String, @Body request: AttestationChallengeRequest): AttestationChallengeResponse

    @POST
    suspend fun register(@Url url: String, @Body request: AttestationRegisterRequest)
}
