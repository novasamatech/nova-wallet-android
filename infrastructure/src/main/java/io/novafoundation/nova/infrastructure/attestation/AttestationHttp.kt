package io.novafoundation.nova.infrastructure.attestation

import io.novafoundation.nova.infrastructure.attestation.profile3.isProfile3BearerDenial
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import retrofit2.HttpException

internal val JSON_MEDIA_TYPE = "application/json".toMediaType()

private val ERROR_CODE = Regex("\"code\"\\s*:\\s*\"([a-z_]+)\"")

/**
 * The backend compares Content-Type as an exact string and accepts only "application/json" or
 * "application/json; charset=utf-8". Gson's converter sends "charset=UTF-8", which is neither, so JSON
 * bodies are relabelled with the bare type - the bytes are UTF-8 either way.
 */
internal fun MediaType?.normalizedForAttestation(): MediaType? {
    return if (this != null && type == "application" && subtype == "json") JSON_MEDIA_TYPE else this
}

internal fun signedContext(method: String, url: HttpUrl, contentType: String): AttestationSigning.RequestContext {
    // The authority carries a port only when it is not the scheme's default, exactly as Traefik rebuilds it
    val authority = if (url.port == HttpUrl.defaultPort(url.scheme)) url.host else "${url.host}:${url.port}"

    return AttestationSigning.RequestContext(
        method = method,
        scheme = url.scheme,
        authority = authority,
        port = url.port.toString(),
        path = url.encodedPath,
        contentType = contentType
    )
}

/** The machine-readable code from the backend's error envelope, `{"error":{"code":...}}`. */
internal fun errorCodeOf(body: String?): String? = body?.let { ERROR_CODE.find(it)?.groupValues?.get(1) }

internal fun HttpException.errorCode(): String? = errorCodeOf(runCatching { response()?.errorBody()?.string() }.getOrNull())

/** Like [errorCode], but leaves the error body unread, so the code can be checked more than once. */
internal fun HttpException.peekErrorCode(): String? = try {
    val source = response()?.errorBody()?.source()
    source?.request(ERROR_PEEK_BYTES)
    source?.buffer?.clone()?.let { errorCodeOf(it.readUtf8(minOf(it.size, ERROR_PEEK_BYTES))) }
} catch (failure: Exception) {
    null
}

private const val ERROR_PEEK_BYTES = 4_096L

// The attestation contract's error codes, the same list the iOS client knows
private val ATTESTATION_ERROR_CODES = setOf(
    "invalid_request", "unsupported_profile", "invalid_target", "unsupported_platform", "unsupported_attestation_type",
    "unknown_client", "invalid_challenge", "invalid_proof", "attestation_failed", "app_not_allowed", "binding_not_allowed",
    "client_already_registered", "request_too_large", "unsupported_media_type", "attestation_unavailable"
)

/** The attestation error code of a failed call, or null when the backend answered without one it defines. Reads the error body. */
fun HttpException.attestationErrorCode(): String? = errorCode()?.takeIf { it in ATTESTATION_ERROR_CODES }

/** Why the backend will not serve a failed call on a later attempt. */
enum class BackendRefusal {

    /** This device cannot attest, or the backend does not trust it. */
    DEVICE,

    /** The backend does not accept the request, or refused this client for the rest of the process. */
    REQUEST
}

/** The backend refusal behind this failure, or null when a later attempt may pass. */
fun Throwable.backendRefusal(): BackendRefusal? {
    val causes = causeChain().toList()
    val refusal = causes.firstNotNullOfOrNull { it as? AttestationRefusedException }
    // The backend's answer to the call itself; below an attestation failure it answered the client's bootstrap instead.
    val responses = causes.takeWhile { it !is AttestationUnavailableException }.filterIsInstance<HttpException>()
    val deviceBound = causes.any { it is AttestationUnsupportedException } || refusal?.isDeviceBound() == true ||
        responses.any { it.isProfile3BearerDenial() }
    return when {
        deviceBound -> BackendRefusal.DEVICE
        refusal != null || responses.any { it.isRejectedCall() } -> BackendRefusal.REQUEST
        else -> null
    }
}

/** How the backend refused a client bootstrap, or null when a bootstrap after the cooldown may pass. */
internal fun bootstrapRefusal(status: Int, code: String?): BackendRefusal? = when (status) {
    401 -> BackendRefusal.DEVICE.takeIf { code == "attestation_failed" }
    403 -> when (code) {
        "app_not_allowed", "binding_not_allowed" -> BackendRefusal.DEVICE
        "insufficient_scope" -> BackendRefusal.REQUEST
        else -> null
    }
    409 -> BackendRefusal.REQUEST.takeIf { code == "client_already_registered" }
    else -> null
}

private fun AttestationRefusedException.isDeviceBound() = bootstrapRefusal(status, code) == BackendRefusal.DEVICE

// The answers graded as rejected; any other error status may pass on a later attempt.
private fun HttpException.isRejectedCall() = when (code()) {
    400, 405, 413, 415, 422 -> true
    403 -> peekErrorCode() == "insufficient_scope"
    404 -> peekErrorCode() == "route_not_found"
    409 -> peekErrorCode() == "client_already_registered"
    else -> false
}

/** This failure followed by its causes. */
fun Throwable.causeChain(): Sequence<Throwable> = generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH)

private const val MAX_CAUSE_DEPTH = 16

/** Applies [normalizedForAttestation] to the backend's own bootstrap endpoints. */
internal class ExactJsonContentTypeInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val body = request.body ?: return chain.proceed(request)

        val normalized = body.contentType().normalizedForAttestation()
        if (normalized == body.contentType()) return chain.proceed(request)

        val bytes = Buffer().apply(body::writeTo).readByteArray()

        return chain.proceed(request.newBuilder().method(request.method, bytes.toRequestBody(normalized)).build())
    }
}
