package io.novafoundation.nova.infrastructure.attestation

import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.PROFILE
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.Purpose
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.base64
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.requestDigest
import io.novafoundation.nova.infrastructure.attestation.AttestationSigning.sha256
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl
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
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_CONFLICT = 409
private const val HTTP_NO_CONTENT = 204
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVICE_UNAVAILABLE = 503
private const val MIN_COOLDOWN_SECONDS = 1L
private const val MAX_COOLDOWN_SECONDS = 905L
private const val DEFAULT_COOLDOWN_SECONDS = 30L
private const val NANOS_PER_SECOND = 1_000_000_000L

/** The backend rejected this client's attestation; retrying will not help until the app restarts. */
class AttestationFailedException(message: String) : IOException(message)

/**
 * Attestation could not be completed this time - the backend answered with an error, or the device
 * key failed - so the request was not sent. Unlike [AttestationFailedException] it says nothing about
 * whether this client is accepted, so it is worth retrying later.
 */
open class AttestationUnavailableException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * This device cannot attest at all - Play Integrity is missing or unusable here - so retrying will not help.
 * Callers should explain the device limitation instead of offering a retry. A backend refusal is not this: it stays an
 * [AttestationUnavailableException] so it is retried and reported like any other failure.
 */
class AttestationUnsupportedException(message: String, cause: Throwable? = null) : AttestationUnavailableException(message, cause)

fun Throwable.isAttestationUnsupported(): Boolean = generateSequence(this) { it.cause }.any { it is AttestationUnsupportedException }

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

    /**
     * Whether a client id may be created and registered. While not allowed, requests that need a new
     * identity fail with [AttestationUnavailableException] instead of minting one - an opted-out user
     * must not get a fresh installation registered behind their back by a request already in flight.
     */
    fun setClientCreationAllowed(allowed: Boolean)

    /**
     * Drops the client id and its key, so the next registration happens as a new installation that
     * cannot be linked to the previous one.
     */
    suspend fun forgetClient()

    suspend fun recordResponse(context: AttestationSigning.RequestContext, status: Int, code: String?, retryAfter: String?) = Unit
}

class RealClientAttestationService(
    private val api: AttestationApi,
    private val identity: AttestationIdentity,
    private val keyPairStore: AttestationKeyPairStore,
    private val integrityTokens: IntegrityTokenSource,
    private val appPackage: String,
    private val urls: InfrastructureUrls,
    private val monotonicNanos: () -> Long = System::nanoTime
) : ClientAttestationService {

    private val registrationMutex = Mutex()

    private val rejectedOrigins = mutableSetOf<String>()

    private val cooldownDeadlines = mutableMapOf<String, Long>()

    @Volatile
    private var clientCreationAllowed = true

    override suspend fun proofHeaders(context: AttestationSigning.RequestContext, body: ByteArray): Map<String, String> {
        // Capture once: remote config may change while bootstrap is suspended.
        val base = urls.baseUrl() ?: throw InfrastructureUrlMissingException()
        val origin = origin(context)
        val configuredContext = signedContext(context.method, base, context.contentType)
        val originMatches = base.scheme == context.scheme && base.port.toString() == context.port && origin == origin(configuredContext)
        if (!originMatches) {
            throw AttestationUnavailableException("Infrastructure origin changed before attestation")
        }
        return registrationMutex.withLock {
            checkCooldown(origin)
            val proof = requestChallenge(base, origin)
            val digest = requestDigest(Purpose.REQUEST, proof.challenge, proof.clientId, context, sha256(body))
            val signature = try {
                keyPairStore.sign(proof.clientId, digest)
            } catch (error: AttestationKeyMissingException) {
                identity.reset()
                throw AttestationUnavailableException("Signing key disappeared; retry with a new installation", error)
            }
            requireClientAllowed()
            mapOf(
                HEADER_PROFILE to PROFILE.toString(),
                HEADER_CLIENT_ID to proof.clientId,
                HEADER_CHALLENGE to proof.challenge,
                HEADER_SIGNATURE to base64(signature)
            )
        }
    }

    override suspend fun recordResponse(context: AttestationSigning.RequestContext, status: Int, code: String?, retryAfter: String?) {
        if (status != HTTP_TOO_MANY_REQUESTS && !(status == HTTP_SERVICE_UNAVAILABLE && code == "attestation_unavailable")) return
        registrationMutex.withLock { applyCooldown(origin(context), retryAfter) }
    }

    private fun origin(context: AttestationSigning.RequestContext): String = "${context.scheme}://${context.authority}"

    private fun checkCooldown(origin: String) {
        val deadline = cooldownDeadlines[origin] ?: return
        if (deadline - monotonicNanos() > 0) throw AttestationUnavailableException("Attestation origin is cooling down")
        cooldownDeadlines.remove(origin)
    }

    private fun applyCooldown(origin: String, retryAfter: String?) {
        val seconds = retryAfter?.takeIf { it.isNotEmpty() && it.all { character -> character in '0'..'9' } }
            ?.toLongOrNull()?.coerceIn(MIN_COOLDOWN_SECONDS, MAX_COOLDOWN_SECONDS) ?: DEFAULT_COOLDOWN_SECONDS
        val now = monotonicNanos()
        val delay = seconds * NANOS_PER_SECOND
        val previousRemaining = cooldownDeadlines[origin]?.minus(now) ?: 0L
        cooldownDeadlines[origin] = now + maxOf(delay, previousRemaining)
    }

    override suspend fun forgetRegistration(clientId: String) {
        registrationMutex.withLock {
            if (identity.existingClientId() == clientId) identity.clearAttested()
        }
    }

    override fun setClientCreationAllowed(allowed: Boolean) {
        clientCreationAllowed = allowed
    }

    override suspend fun forgetClient() {
        // Under the mutex, so a registration in progress finishes before its identity is dropped
        registrationMutex.withLock {
            val clientId = identity.existingClientId() ?: return

            identity.reset()
            keyPairStore.delete(clientId)
            attestationLog("client forgotten - the next registration starts as a new installation")
        }
    }

    /** A request-purpose challenge together with the identity it was issued to. */
    private suspend fun requestChallenge(base: HttpUrl, origin: String): RequestProof {
        ensureAttested(base, origin)
        val clientId = identity.clientId()

        return try {
            val challenge = challenge(base, clientId, Purpose.REQUEST)
            if (identity.attestedOrigin() == null) {
                // A shipped registration has no origin metadata. Adopt only the origin that
                // just acknowledged this client; do not re-attest or guess its former origin.
                requireClientAllowed()
                identity.markAttested(origin)
            }
            RequestProof(clientId, challenge)
        } catch (exception: HttpException) {
            val error = exception.backendError()
            if (error.status != HTTP_UNAUTHORIZED || error.code != ERROR_UNKNOWN_CLIENT) throw challengeFailure(exception, error, origin)

            attestationWarn("challenge: the backend does not know this installation - registering it again")
            identity.clearAttested()
            ensureAttested(base, origin)

            val current = identity.clientId()
            try {
                RequestProof(current, challenge(base, current, Purpose.REQUEST))
            } catch (retryException: HttpException) {
                throw challengeFailure(retryException, retryException.backendError(), origin)
            }
        }
    }

    private suspend fun challenge(base: HttpUrl, clientId: String, purpose: Purpose): String {
        val request = AttestationChallengeRequest(client_id = clientId, purpose = purpose.wireName, profile = PROFILE)

        return api.challenge(requireNotNull(base.resolve(CHALLENGES_PATH)).toString(), request).challenge
    }

    private fun challengeFailure(exception: HttpException, error: BackendError, origin: String): IOException {
        if (error.status == HTTP_TOO_MANY_REQUESTS || (error.status == HTTP_SERVICE_UNAVAILABLE && error.code == "attestation_unavailable")) {
            applyCooldown(origin, exception.response()?.headers()?.get("Retry-After"))
        }
        // app_not_allowed / binding_not_allowed: policy, not a transient fault - stop asking
        return if (error.status == HTTP_FORBIDDEN) {
            rejectedOrigins.add(origin)
            attestationWarn("challenge refused with $error - signing is off until restart")
            AttestationFailedException("Challenge refused: $error")
        } else {
            attestationWarn("challenge failed with $error, retried on the next request")
            AttestationUnavailableException("Challenge failed: $error", exception)
        }
    }

    private suspend fun ensureAttested(base: HttpUrl, origin: String) {
        if (origin in rejectedOrigins) {
            attestationWarn("refusing to sign: attestation was rejected earlier in this process")
            throw AttestationFailedException("Attestation was rejected by the backend")
        }
        requireClientAllowed()
        val existing = identity.existingClientId()
        if (existing != null && identity.isAttested() && !keyPairStore.contains(existing)) {
            // The backend cannot rebind an existing id to a different key.
            identity.reset()
        }
        val registeredOrigin = identity.attestedOrigin()
        if (identity.isAttested() && (registeredOrigin == null || registeredOrigin == origin)) return
        identity.clearAttested()
        register(base, origin, identity.clientId())
        requireClientAllowed()
        identity.markAttested(origin)
        attestationLog("registered package=$appPackage")
    }

    // Checked before touching the identity: reading the client id would mint a new one
    private fun requireClientAllowed() {
        if (clientCreationAllowed) return

        attestationLog("refusing to sign: client creation is not allowed right now")
        throw AttestationUnavailableException("Client creation is not allowed")
    }

    private suspend fun register(base: HttpUrl, origin: String, clientId: String) {
        // The signed context must name exactly the URL the request goes to, so both come from here
        val url = requireNotNull(base.resolve(REGISTER_PATH))
        val keyReference = base64(keyPairStore.publicKey(clientId))

        attestationLog("registering package=$appPackage")

        // Which call a failure came from: the two share one error table, so the code alone cannot say
        var step = "challenge"

        try {
            val challenge = challenge(base, clientId, Purpose.REGISTER)
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
            val response = api.register(
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
            if (!response.isSuccessful) throw HttpException(response)
            if (response.code() != HTTP_NO_CONTENT) {
                throw AttestationUnavailableException("Unexpected registration acknowledgement: HTTP ${response.code()}")
            }
        } catch (exception: HttpException) {
            throw registrationFailure(exception, clientId, step, origin)
        }
    }

    /** Called with [registrationMutex] held. */
    private fun registrationFailure(exception: HttpException, clientId: String, step: String, origin: String): IOException {
        val error = exception.backendError()
        val code = error.code
        val status = "$error on $step with message: ${exception.message()}"

        return when {
            error.status == HTTP_TOO_MANY_REQUESTS || (error.status == HTTP_SERVICE_UNAVAILABLE && code == "attestation_unavailable") -> {
                applyCooldown(origin, exception.response()?.headers()?.get("Retry-After"))
                AttestationUnavailableException("Attestation temporarily throttled: $status", exception)
            }

            error.status == HTTP_CONFLICT && code == ERROR_CLIENT_ALREADY_REGISTERED -> {
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
                rejectedOrigins.add(origin)
                attestationWarn("registration rejected with \"$status\" - signing is off until restart")
                AttestationFailedException("Attestation rejected: $status")
            }
        }
    }

    /** Reads the error body, which can be read only once - take everything needed from this one value. */
    private fun HttpException.backendError() = BackendError(code(), errorCode())

    private class BackendError(val status: Int, val code: String?) {

        override fun toString() = "HTTP $status" + code?.let { " $it" }.orEmpty()
    }

    private class RequestProof(val clientId: String, val challenge: String)
}
