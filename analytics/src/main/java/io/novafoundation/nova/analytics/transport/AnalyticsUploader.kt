package io.novafoundation.nova.analytics.transport

import io.novafoundation.nova.analytics.analyticsLog
import io.novafoundation.nova.analytics.analyticsWarn
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.attestation.attestationErrorCode
import io.novafoundation.nova.infrastructure.resolve
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import retrofit2.HttpException

private const val PLATFORM_ANDROID = "android"
private const val HTTP_BAD_REQUEST = 400

// The backend will refuse these bytes every time: retrying only blocks the queue behind them
private val HTTP_POISON_BATCH = setOf(413, 415, 422)

private const val ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

class AnalyticsUploader(
    private val api: AnalyticsApi,
    private val urls: InfrastructureUrls,
    private val identity: AnalyticsIdentity,
    private val queue: AnalyticsEventQueue,
    private val appVersion: String,
    private val batchSize: Int
) {

    /**
     * Sends up to [maxBatches] batches, oldest first. Mirrors the iOS client: a batch the backend can never accept
     * is dropped and the flush moves on; any other failure keeps the batch and ends the flush.
     */
    suspend fun flush(maxBatches: Int): Result<Unit> {
        repeat(maxBatches) {
            val batch = queue.peek(batchSize)
            if (batch.isEmpty()) {
                analyticsLog("flush: queue empty")
                return Result.success(Unit)
            }

            analyticsLog("sending ${batch.size} events: ${batch.joinToString { it.name }}")

            val error = runCatching { api.sendEvents(urls.resolve(EVENTS_PATH).toString(), createEnvelope(batch)) }.exceptionOrNull()

            when {
                error == null -> analyticsLog("delivered ${batch.size} events")

                error.isPoisonBatch() -> analyticsWarn("upload failed: ${error.describe()} - the backend refuses this batch, dropped")

                else -> {
                    analyticsWarn("upload failed: ${error.describe()} - kept for the next flush")
                    return Result.failure(error)
                }
            }

            queue.drop(batch.size)

            // A partial batch was the rest of the queue
            if (batch.size < batchSize) return Result.success(Unit)
        }

        return Result.success(Unit)
    }

    private fun createEnvelope(batch: List<QueuedEvent>): AnalyticsEventsRequest {
        return AnalyticsEventsRequest(
            v = ANALYTICS_SCHEMA_VERSION,
            platform = PLATFORM_ANDROID,
            app_version = appVersion,
            install_id = identity.installId(),
            session_id = identity.sessionId,
            sent_at = formatIso8601(System.currentTimeMillis()),
            events = batch.map { AnalyticsEventRequest(it.id, it.name, formatIso8601(it.timestamp), it.props) }
        )
    }

    // 400 with an attestation code is a refused proof, not a malformed batch: the same events may pass with a fresh one
    private fun Throwable.isPoisonBatch(): Boolean {
        if (this !is HttpException) return false

        return when (code()) {
            HTTP_BAD_REQUEST -> attestationErrorCode() == null
            in HTTP_POISON_BATCH -> true
            else -> false
        }
    }

    private fun Throwable.describe(): String {
        val status = (this as? HttpException)?.code()?.let { "HTTP $it" } ?: javaClass.simpleName
        return "$status ${message.orEmpty()}"
    }

    private fun formatIso8601(millis: Long): String {
        val formatter = SimpleDateFormat(ISO_8601, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return formatter.format(Date(millis))
    }
}
