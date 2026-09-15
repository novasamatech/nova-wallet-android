package io.novafoundation.nova.infrastructure.attestation

import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.PROFILE
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.Purpose
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.base64
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.requestDigest
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.sha256
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.resolve
import retrofit2.HttpException

const val HEADER_PROFILE = "X-Attestation-Profile"
const val HEADER_CLIENT_ID = "X-Client-Id"
const val HEADER_CHALLENGE = "X-Challenge"
const val HEADER_SIGNATURE = "X-Signature"

internal const val ERROR_UNKNOWN_CLIENT = "unknown_client"
internal const val ERROR_INVALID_CHALLENGE = "invalid_challenge"
private const val ERROR_CLIENT_ALREADY_REGISTERED = "client_already_registered"

private const val PLATFORM_ANDROID = "android"
private const val TYPE_PLAY_INTEGRITY = "play_integrity"

// Only App Attest has environments; Android sends it empty, and it is still part of the binding
private const val NO_APP_ATTEST_ENVIRONMENT = ""

private const val HTTP_FORBIDDEN = 403
private const val HTTP_SERVER_ERRORS = 500

/** The backend rejected this client's attestation; retrying will not help until the app restarts. */
class AttestationFailedException(message: String) : IOException(message)

/**
 * Attestation could not be completed this time - the backend answered with an error, or the device
 * key failed - so the request was not sent. Unlike [AttestationFailedException] it says nothing about
 * whether this client is accepted, so it is worth retrying later.
 */
class AttestationUnavailableException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Produces a Play Integrity token bound to [requestHash]. */
fun interface IntegrityTokenSource {

    suspend fun token(requestHash: String): String
}

interface ClientAttestationService {

    /**
     * Proof headers for the one request described by [context] and [body], Registers the installation first if needed. Each call spends a fresh challenge - a proof
     * is good for exactly one request.
     */
    suspend fun proofHeaders(context: AttestationSigning.RequestContext, body: ByteArray): Map<String, String>

    /**
     * The backend no longer knows [clientId] - typically its database was reset. The identity and key are
     * still ours, so the next proof registers them again rather than minting new ones. Ignored when the
     * identity has already moved on, so a burst of such answers triggers one recovery.
     */
    suspend fun forgetRegistration(clientId: String)
}

class RealClientAttestationService(
    private val api: AttestationApi,
    private val identity: AttestationIdentity,
    private val keyPairStore: AttestationKeyPairStore,
    private val integrityTokens: IntegrityTokenSource,
    private val appPackage: String,
    private val urls: InfrastructureUrls
) : ClientAttestationService {

    private val registrationMutex = Mutex()

    @Volatile
    private var rejected = false

    override suspend fun proofHeaders(context: AttestationSigning.RequestContext, body: ByteArray): Map<String, String> {
        val (clientId, challenge) = requestChallenge()
        val digest = requestDigest(Purpose.REQUEST, challenge, clientId, context, sha256(body))

        return mapOf(
            HEADER_PROFILE to PROFILE.toString(),
            HEADER_CLIENT_ID to clientId,
            HEADER_CHALLENGE to challenge,
            HEADER_SIGNATURE to base64(keyPairStore.sign(clientId, digest))
        )
    }

    override suspend fun forgetRegistration(clientId: String) {
        registrationMutex.withLock {
            if (identity.clientId() == clientId) identity.clearAttested()
        }
    }

    /** A request-purpose challenge together with the identity it was issued to. */
    private suspend fun requestChallenge(): Pair<String, String> {
        ensureAttested()
        val clientId = identity.clientId()

        return try {
            clientId to challenge(clientId, Purpose.REQUEST)
        } catch (exception: HttpException) {
            val error = exception.backendError()
            if (error.code != ERROR_UNKNOWN_CLIENT) throw challengeFailure(exception, error)

            attestationWarn("challenge: the backend does not know this installation - registering it again")
            forgetRegistration(clientId)
            ensureAttested()

            val current = identity.clientId()
            try {
                current to challenge(current, Purpose.REQUEST)
            } catch (retryException: HttpException) {
                throw challengeFailure(retryException, retryException.backendError())
            }
        }
    }

    private suspend fun challenge(clientId: String, purpose: Purpose): String {
        val request = AttestationChallengeRequest(client_id = clientId, purpose = purpose.wireName, profile = PROFILE)

        return api.challenge(urls.resolve(CHALLENGES_PATH).toString(), request).challenge
    }

    private fun challengeFailure(exception: HttpException, error: BackendError): IOException {
        // app_not_allowed / binding_not_allowed: policy, not a transient fault - stop asking
        return if (error.status == HTTP_FORBIDDEN) {
            rejected = true
            attestationWarn("challenge refused with $error - signing is off until restart")
            AttestationFailedException("Challenge refused: $error")
        } else {
            attestationWarn("challenge failed with $error, retried on the next request")
            AttestationUnavailableException("Challenge failed: $error", exception)
        }
    }

    private suspend fun ensureAttested() {
        if (rejected) {
            attestationWarn("refusing to sign: attestation was rejected earlier in this process")
            throw AttestationFailedException("Attestation was rejected by the backend")
        }
        if (identity.isAttested()) return

        registrationMutex.withLock {
            if (identity.isAttested()) return

            register(identity.clientId())
            identity.markAttested()
            attestationLog("registered package=$appPackage")
        }
    }

    private suspend fun register(clientId: String) {
        // The signed context must name exactly the URL the request goes to, so both come from here
        val url = urls.resolve(REGISTER_PATH)
        val keyReference = base64(keyPairStore.publicKey(clientId))

        attestationLog("registering package=$appPackage")

        // Which call a failure came from: the two share one error table, so the code alone cannot say
        var step = "challenge"

        try {
            val challenge = challenge(clientId, Purpose.REGISTER)
            val binding = AttestationSigning.registrationPreimage(
                platform = PLATFORM_ANDROID,
                appId = appPackage,
                attestationType = TYPE_PLAY_INTEGRITY,
                keyReference = keyReference,
                appAttestEnvironment = NO_APP_ATTEST_ENVIRONMENT
            )
            val context = signedContext("POST", url, JSON_MEDIA_TYPE.toString())
            val digest = requestDigest(Purpose.REGISTER, challenge, clientId, context, sha256(binding))

            step = "register"
            api.register(
                url.toString(),
                AttestationRegisterRequest(
                    profile = PROFILE,
                    client_id = clientId,
                    challenge = challenge,
                    platform = PLATFORM_ANDROID,
                    app_id = appPackage,
                    attestation_type = TYPE_PLAY_INTEGRITY,
                    key_reference = keyReference,
                    app_attest_environment = NO_APP_ATTEST_ENVIRONMENT,
                    integrity_token = integrityTokens.token(AttestationSigning.requestHash(digest)),
                    signature = base64(keyPairStore.sign(clientId, digest))
                )
            )
        } catch (exception: HttpException) {
            throw registrationFailure(exception, clientId, step)
        }
    }

    /** Called with [registrationMutex] held. */
    private fun registrationFailure(exception: HttpException, clientId: String, step: String): IOException {
        val error = exception.backendError()
        val code = error.code
        val status = "$error on $step"

        return when {
            code == ERROR_CLIENT_ALREADY_REGISTERED -> {
                // This id is bound to another key - ours was lost while the id survived. A binding is never
                // overwritten, so the only way forward is to start over as a new installation.
                identity.reset()
                keyPairStore.delete(clientId)
                attestationWarn("registration conflicts with an existing binding ($status) - starting over as a new installation")
                AttestationUnavailableException("Registration conflict: $status", exception)
            }

            code == ERROR_INVALID_CHALLENGE || error.status >= HTTP_SERVER_ERRORS -> {
                attestationWarn("registration failed with $status, retried on the next request")
                AttestationUnavailableException("Registration failed: $status", exception)
            }

            else -> {
                // attestation_failed, app_not_allowed, invalid_request...: repeating it would only burn the
                // backend's daily Play Integrity budget
                rejected = true
                attestationWarn("registration rejected with $status - signing is off until restart")
                AttestationFailedException("Attestation rejected: $status")
            }
        }
    }

    /** Reads the error body, which can be read only once - take everything needed from this one value. */
    private fun HttpException.backendError() = BackendError(code(), errorCode())

    private class BackendError(val status: Int, val code: String?) {

        override fun toString() = "HTTP $status" + code?.let { " $it" }.orEmpty()
    }
}
