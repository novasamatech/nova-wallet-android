package io.novafoundation.nova.infrastructure.attestation

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer

/**
 * Signs every request passing through with attestation headers. Attach to the
 * OkHttp client of any backend API that sits behind attestation — the first
 * request also performs the one-time client registration.
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

        val headers = runBlocking { attestationService.signedHeaders(bodyBytes) }
            ?: return chain.proceed(request)

        val signed = request.newBuilder()
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()

        return chain.proceed(signed)
    }
}
