package io.novafoundation.nova.infrastructure.attestation

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
