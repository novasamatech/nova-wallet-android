package io.novafoundation.nova.infrastructure.attestation

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer

private const val HTTP_UNAUTHORIZED = 401
private const val ERROR_PEEK_BYTES = 1024L

/**
 * Attaches a profile 2 proof to every request passing through. Attach to the OkHttp client of any backend
 * API behind attestation; the first request also registers the installation.
 *
 * Attestation's own endpoints must use a client without this interceptor.
 *
 * Note for anyone reading HTTP logs: this runs after the app's HttpLoggingInterceptor, so those logs show
 * requests without proof headers and only the final response. The NovaAttestation tag shows the rest.
 */
class AttestationInterceptor(
    private val attestationService: ClientAttestationService
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val target = "${original.method} ${original.url.host}${original.url.encodedPath}"

        val body = original.body
        if (body != null && (body.isDuplex() || body.isOneShot())) {
            attestationLog("$target: streaming body, sent unsigned")
            return chain.proceed(original)
        }

        // Serialize once and send exactly what was signed: the backend hashes the bytes it receives, and
        // compares the Content-Type it sees with the one in the proof
        val bodyBytes = body?.let { Buffer().apply(it::writeTo).readByteArray() } ?: ByteArray(0)
        val contentType = body?.contentType().normalizedForAttestation()
        val request = original.newBuilder().method(original.method, body?.let { bodyBytes.toRequestBody(contentType) }).build()
        val context = signedContext(request.method, request.url, contentType?.toString().orEmpty())

        val headers = attest(target) { attestationService.proofHeaders(context, bodyBytes) }
        if (headers == null) {
            attestationLog("$target: attestation disabled in this build, sent unsigned")
            return chain.proceed(request)
        }

        val response = chain.proceed(request.signedWith(headers))
        attestationLog("$target: ${response.describe()}")

        if (response.code != HTTP_UNAUTHORIZED) return response

        when (val code = errorCodeOf(response.peekBody(ERROR_PEEK_BYTES).string())) {
            // Expired or already spent - the only fix is a fresh one, the registration is fine
            ERROR_INVALID_CHALLENGE -> attestationLog("$target: challenge no longer valid, one more try with a fresh one")

            ERROR_UNKNOWN_CLIENT -> {
                attestationWarn("$target: the backend does not know this installation, registering it again")
                attest(target) { attestationService.forgetRegistration(headers.getValue(HEADER_CLIENT_ID)) }
            }

            // invalid_proof, attestation_failed...: another attempt would fail the same way
            else -> {
                attestationWarn("$target: 401 ${code ?: "without an error code"}, not retried")
                return response
            }
        }

        response.close()

        val retryHeaders = attest(target) { attestationService.proofHeaders(context, bodyBytes) }
            ?: return chain.proceed(request)

        return chain.proceed(request.signedWith(retryHeaders)).also {
            attestationLog("$target: retry ${it.describe()}")
        }
    }

    /**
     * Runs attestation work for one request and makes sure whatever goes wrong leaves as an IOException.
     *
     * Not tidiness: OkHttp hands any other exception thrown by an interceptor to the caller as a
     * cancellation, then rethrows it on its dispatcher thread, where nothing catches it and the process
     * dies. A backend error while fetching a challenge is exactly such an exception - Retrofit reports
     * HTTP errors as HttpException - so a single 5xx, or a 415, used to crash the app.
     */
    private fun <T> attest(target: String, work: suspend () -> T): T {
        return try {
            runBlocking { work() }
        } catch (error: IOException) {
            attestationWarn("$target: could not sign - ${error.describe()}")
            throw error
        } catch (error: Exception) {
            attestationWarn("$target: could not sign - ${error.describe()}")
            throw AttestationUnavailableException("Could not attest $target: ${error.describe()}", error)
        }
    }

    private fun Request.signedWith(headers: Map<String, String>): Request {
        return newBuilder()
            .apply { headers.forEach { (name, value) -> header(name, value) } }
            .build()
    }

    // The backend's request id is what to quote to its maintainers; proofs and installation ids are never logged
    private fun Response.describe(): String = "HTTP $code" + header("x-request-id")?.let { " (x-request-id $it)" }.orEmpty()

    private fun Throwable.describe(): String = "${javaClass.simpleName}: ${message.orEmpty().trim()}"
}
