package io.novafoundation.nova.infrastructure.attestation.profile3

import androidx.annotation.Keep
import com.google.gson.annotations.JsonAdapter

@Keep
class Profile3ChallengeRequest(val client_id: String, val purpose: String, val profile: Int = Profile3Signing.PROFILE)

@Keep
@JsonAdapter(Profile3ChallengeResponseAdapter::class)
class Profile3ChallengeResponse(val challenge: String?, val issued_at: Long?, val expires_at: Long?)

@Keep
class Profile3RegisterRequest(
    val client_id: String,
    val challenge: String,
    val app_id: String,
    val key_reference: String,
    val integrity_token: String,
    val signature: String,
    val profile: Int = Profile3Signing.PROFILE,
    val platform: String = "android",
    val attestation_type: String = "play_integrity",
    val app_attest_environment: String = ""
)

@Keep
class Profile3TokenRequest(val client_id: String, val challenge: String, val signature: String, val profile: Int = Profile3Signing.PROFILE)

@Keep
@JsonAdapter(Profile3TokenResponseAdapter::class)
class Profile3TokenResponse(val accessToken: String, val issuedAt: Long, val expiresAt: Long)
