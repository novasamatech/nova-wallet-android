package io.novafoundation.nova.analytics.transport

import io.novafoundation.nova.analytics.analyticsLog
import io.novafoundation.nova.analytics.analyticsWarn
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.attestation.AttestationFailedException
import io.novafoundation.nova.infrastructure.resolve
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
    private val urls: InfrastructureUrls,
    private val identity: AnalyticsIdentity,
    private val queue: AnalyticsEventQueue,
    private val appVersion: String,
    private val batchSize: Int
) {

    suspend fun flush(): Result<Unit> {
        val result = runCatching {
            while (true) {
                val batch = queue.peek(batchSize)
                if (batch.isEmpty()) {
                    analyticsLog("flush: queue empty")
                    return@runCatching
                }

                analyticsLog("sending ${batch.size} events: ${batch.joinToString { it.name }}")
                api.sendEvents(urls.resolve(EVENTS_PATH).toString(), createEnvelope(batch))
                queue.drop(batch.size)
                analyticsLog("delivered ${batch.size} events")
            }
        }

        result.exceptionOrNull()?.let { error ->
            val permanent = isPermanentRejection(error)
            val status = (error as? HttpException)?.code()?.let { "HTTP $it" } ?: error.javaClass.simpleName

            analyticsWarn(
                "upload failed: $status ${error.message.orEmpty()} - " +
                    if (permanent) "permanent, queue cleared" else "kept for the next flush"
            )

            if (permanent) queue.clear()
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
