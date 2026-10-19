package io.novafoundation.nova.infrastructure.attestation.profile3

import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.attestation.AttestationKeyPairStore
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning
import io.novafoundation.nova.infrastructure.attestation.IntegrityTokenSource
import io.novafoundation.nova.infrastructure.attestation.signedContext
import java.io.IOException
import okhttp3.HttpUrl

class Profile3RegistrationService(
    private val api: Profile3Api,
    private val keys: AttestationKeyPairStore,
    private val integrityTokens: IntegrityTokenSource,
    private val appPackage: String,
    private val urls: InfrastructureUrls,
    private val identities: Profile3IdentityStore
) {

    suspend fun register(identity: Profile3Identity, origin: HttpUrl) {
        requireConfiguredOrigin(urls, origin)
        val challenge = requireProfile3Challenge(
            api.challenge(endpoint(origin, "challenges"), Profile3ChallengeRequest(identity.scope.clientId, "register"))
        )
        requireConfiguredOrigin(urls, origin)
        val publicKey = AttestationSigning.base64(keys.publicKey(identity.keyAlias))
        identities.markKeyCreated(identity.scope)
        val binding = Profile3Signing.registrationPreimage("android", appPackage, "play_integrity", publicKey, "")
        val url = endpoint(origin, "register")
        val context = signedContext("POST", requireNotNull(origin.resolve(url)), "application/json")
        val digest = Profile3Signing.requestDigest(
            Profile3Signing.Purpose.REGISTER,
            challenge,
            identity.scope.clientId,
            context,
            AttestationSigning.sha256(binding)
        )
        val integrityToken = integrityTokens.token(AttestationSigning.requestHash(digest))
        requireConfiguredOrigin(urls, origin)
        val request = Profile3RegisterRequest(
            client_id = identity.scope.clientId,
            challenge = challenge,
            app_id = appPackage,
            key_reference = publicKey,
            integrity_token = integrityToken,
            signature = AttestationSigning.base64(keys.sign(identity.keyAlias, digest))
        )
        val response = api.register(url, request)
        if (!response.isSuccessful) throw retrofit2.HttpException(response)
        if (response.code() != 204 || response.body() != null) throw IOException("Invalid registration acknowledgement")
    }
}

internal fun endpoint(origin: HttpUrl, operation: String): String =
    requireNotNull(origin.resolve("/v1/attestation/profile-3/$operation")).toString()

internal fun canonicalProfile3Origin(url: HttpUrl): String {
    val invalidOrigin = url.scheme != "https" || url.username.isNotEmpty() || url.password.isNotEmpty() ||
        url.host.endsWith('.') || url.host.contains(':') || url.host.all { it.isDigit() || it == '.' }
    if (invalidOrigin) {
        throw IOException("Invalid profile-3 origin")
    }
    val port = if (url.port == 443) "" else ":${url.port}"
    return "https://${url.host}$port"
}

internal suspend fun requireConfiguredOrigin(urls: InfrastructureUrls, origin: HttpUrl) {
    val current = urls.baseUrl() ?: throw IOException("Infrastructure origin is unavailable")
    if (canonicalProfile3Origin(current) != canonicalProfile3Origin(origin)) throw IOException("Infrastructure origin changed")
}

internal fun requireProfile3Challenge(response: Profile3ChallengeResponse): String {
    val challenge = response.challenge ?: throw IOException("Missing profile-3 challenge")
    val issued = response.issued_at ?: throw IOException("Missing challenge timestamp")
    val expires = response.expires_at ?: throw IOException("Missing challenge expiry")
    if (!challenge.matches(Regex("[A-Za-z0-9_-]{43}")) || issued < 0 || issued > Long.MAX_VALUE - 60 || expires != issued + 60) {
        throw IOException("Invalid profile-3 challenge")
    }
    return challenge
}
