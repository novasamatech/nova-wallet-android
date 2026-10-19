package io.novafoundation.nova.infrastructure.attestation.profile3

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import io.novafoundation.nova.infrastructure.attestation.HEADER_CHALLENGE
import io.novafoundation.nova.infrastructure.attestation.HEADER_CLIENT_ID
import io.novafoundation.nova.infrastructure.attestation.HEADER_PROFILE
import io.novafoundation.nova.infrastructure.attestation.HEADER_SIGNATURE
import java.io.IOException
import java.io.InterruptedIOException
import java.io.StringReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer

class Profile3Interceptor(
    private val provider: Profile3AccessTokenProvider,
    private val gson: Gson,
    private val bootstrapTimeoutMillis: Long = 150_000L
) : Interceptor {

    private val errorAdapter = gson.getAdapter(ErrorEnvelope::class.java)

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val body = original.body
        val replayable = body?.let { !it.isOneShot() && !it.isDuplex() } ?: true
        val request = if (body != null && replayable) {
            val bytes = Buffer().apply(body::writeTo).readByteArray()
            original.newBuilder().method(original.method, bytes.toRequestBody(body.contentType())).build()
        } else {
            original
        }
        var record = blocking(chain) { provider.token(request.url) }
        var response = chain.proceed(request.withBearer(record))
        try {
            recordResponse(chain, record, response)
            if (!isRejected(response, record)) return response
            blocking(chain) { provider.invalidate(record) }
            if (!replayable) return response
            response.close()
            record = blocking(chain) { provider.token(request.url) }
            response = chain.proceed(request.withBearer(record))
            recordResponse(chain, record, response)
            if (isRejected(response, record)) {
                blocking(chain) { provider.invalidate(record) }
            }
            return response
        } catch (failure: Exception) {
            response.close()
            throw failure
        }
    }

    private fun Request.withBearer(record: Profile3TokenRecord): Request {
        if (canonicalProfile3Origin(url) != record.scope.origin) throw IOException("Profile-3 bearer origin mismatch")
        return newBuilder()
            .removeHeader(HEADER_PROFILE)
            .removeHeader(HEADER_CLIENT_ID)
            .removeHeader(HEADER_CHALLENGE)
            .removeHeader(HEADER_SIGNATURE)
            .removeHeader("X-App-Attest-Assertion")
            .header("Authorization", "Bearer ${record.accessToken}")
            .build()
    }

    private fun isRejected(response: Response, record: Profile3TokenRecord): Boolean {
        if (response.code != 401 || canonicalProfile3Origin(response.request.url) != record.scope.origin) return false
        val challenges = response.headers.values("WWW-Authenticate")
        if (challenges.size != 1 || !challenges.single().equals("Bearer error=\"invalid_token\"", ignoreCase = true)) return false
        return response.request.method == "HEAD" || errorCode(response) == "invalid_token"
    }

    private fun errorCode(response: Response): String? = try {
        val body = response.peekBody(ERROR_PEEK_BYTES)
        if (body.contentType()?.subtype != "json") {
            null
        } else {
            JsonReader(StringReader(body.string())).use { reader ->
                reader.isLenient = false
                val envelope = errorAdapter.read(reader)
                if (reader.peek() == JsonToken.END_DOCUMENT) envelope?.error?.code else null
            }
        }
    } catch (failure: Exception) {
        null
    }

    private fun recordResponse(chain: Interceptor.Chain, record: Profile3TokenRecord, response: Response) {
        if (response.code != 429 && response.code != 503) return
        blocking(chain) { provider.recordResponse(record, response.code, errorCode(response), response.header("Retry-After")) }
    }

    private fun <T> blocking(chain: Interceptor.Chain, work: suspend () -> T): T = try {
        runBlocking {
            withTimeout(bootstrapTimeoutMillis) {
                if (chain.call().isCanceled()) throw CancellationException("HTTP call cancelled")
                val result = async { work() }
                val cancellation = launch {
                    while (isActive) {
                        if (chain.call().isCanceled()) {
                            result.cancel()
                            break
                        }
                        delay(50)
                    }
                }
                try {
                    result.await()
                } finally {
                    cancellation.cancel()
                }
            }
        }
    } catch (cancelled: CancellationException) {
        throw InterruptedIOException("Profile-3 request cancelled or timed out").apply { initCause(cancelled) }
    } catch (failure: IOException) {
        throw failure
    } catch (failure: Exception) {
        throw IOException("Profile-3 authentication failed", failure)
    }

    @androidx.annotation.Keep
    private class ErrorEnvelope(val error: ErrorDetail?)

    @androidx.annotation.Keep
    private class ErrorDetail(val code: String?)

    private companion object {
        const val ERROR_PEEK_BYTES = 4_096L
    }
}

/** Runs after the native HTTP cache, so a rate cooldown does not discard a fresh cached response. */
class Profile3NetworkAdmission(private val provider: Profile3AccessTokenProvider) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            runBlocking { provider.beforeNetwork(chain.request().url) }
        } catch (failure: IOException) {
            throw failure
        } catch (failure: Exception) {
            throw IOException("Profile-3 network admission failed", failure)
        }
        return chain.proceed(chain.request())
    }
}
