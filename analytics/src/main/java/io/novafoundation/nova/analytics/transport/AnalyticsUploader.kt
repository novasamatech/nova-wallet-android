package io.novafoundation.nova.analytics.transport

import io.novafoundation.nova.infrastructure.attestation.AttestationFailedException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import retrofit2.HttpException

private const val PLATFORM_ANDROID = "android"
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403

private const val ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

class AnalyticsUploader(
    private val api: AnalyticsApi,
    private val identity: AnalyticsIdentity,
    private val queue: AnalyticsEventQueue,
    private val appVersion: String,
    private val batchSize: Int
) {

    suspend fun flush(): Result<Unit> {
        val result = runCatching {
            while (true) {
                val batch = queue.peek(batchSize)
                if (batch.isEmpty()) return@runCatching

                api.sendEvents(createEnvelope(batch))
                queue.drop(batch.size)
            }
        }

        result.exceptionOrNull()?.let { error ->
            if (isPermanentRejection(error)) queue.clear()
        }

        return result
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

    private fun isPermanentRejection(error: Throwable): Boolean {
        val rejected = error is HttpException && error.code() in setOf(HTTP_UNAUTHORIZED, HTTP_FORBIDDEN)

        return rejected || error is AttestationFailedException
    }

    private fun formatIso8601(millis: Long): String {
        val formatter = SimpleDateFormat(ISO_8601, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return formatter.format(Date(millis))
    }
}
