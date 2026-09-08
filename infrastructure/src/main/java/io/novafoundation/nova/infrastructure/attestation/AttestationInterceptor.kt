package io.novafoundation.nova.infrastructure.attestation

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer

private const val HTTP_UNAUTHORIZED = 401

/**
 * Signs every request passing through with attestation headers. Attach to the OkHttp client of any
 * backend API that sits behind attestation - the first request also performs the one-time client
 * registration, and a rejected client is registered again under a fresh identity.
 *
 * Attestation's own endpoints must use a client without this interceptor.
 */
class AttestationInterceptor(
    private val attestationService: ClientAttestationService
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val body = request.body
        if (body != null && (body.isDuplex() || body.isOneShot())) return chain.proceed(request)

        val bodyBytes = body?.let { Buffer().apply(it::writeTo).readByteArray() } ?: ByteArray(0)

        val headers = runBlocking { attestationService.signedHeaders(bodyBytes) } ?: return chain.proceed(request)

        val response = chain.proceed(request.signedWith(headers))

        if (response.code == HTTP_UNAUTHORIZED) {
            // The backend no longer accepts this client: its binding is gone, or it no longer
            // matches our key. Nothing about the current identity can fix that, so it is dropped
            // and rebuilt, and the request gets exactly one more attempt - never a loop.
            response.close()

            val retryHeaders = runBlocking {
                attestationService.invalidate(headers.getValue(HEADER_CLIENT_ID))

                attestationService.signedHeaders(bodyBytes)
            } ?: return chain.proceed(request)

            return chain.proceed(request.signedWith(retryHeaders))
        }

        return response
    }

    private fun Request.signedWith(headers: Map<String, String>): Request {
        return newBuilder()
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
    }
}
