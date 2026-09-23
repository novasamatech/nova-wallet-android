package io.novafoundation.nova.analytics.transport

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsFlushReason
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.analyticsLog
import io.novafoundation.nova.infrastructure.attestation.ClientAttestationService
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Flush schedule mirrors the iOS client. There is no timer: every tracked event checks whether a flush is due -
 * a full batch once 15 s have passed since the last flush, or anything at all once 5 minutes have.
 * Launch and backgrounding flush explicitly.
 */
private const val FLUSH_THRESHOLD_MIN_INTERVAL_MILLIS = 15 * 1000L
private const val FLUSH_INTERVAL_MILLIS = 5 * 60 * 1000L

private const val MAX_BATCHES_PER_FLUSH = 10

// The OS gives a backgrounded app little time, one request is what fits
private const val MAX_BATCHES_IN_BACKGROUND = 1

class RealAnalyticsService(
    private val scope: CoroutineScope,
    private val queue: AnalyticsEventQueue,
    private val uploader: AnalyticsUploader,
    private val identity: AnalyticsIdentity,
    private val attestation: ClientAttestationService,
    private val flushThreshold: Int,
    private val clock: () -> Long = System::currentTimeMillis
) : AnalyticsService {

    private val flushMutex = Mutex()

    @Volatile
    private var lastFlushAt = 0L

    @Volatile
    override var isEnabled: Boolean = false
        set(value) {
            field = value
            analyticsLog("enabled=$value" + if (!value) " - install id and attestation client reset, queue cleared" else "")

            // Set right away, before any coroutine runs: a flush already in flight must not register a new client
            attestation.setClientCreationAllowed(value)

            if (!value) {
                identity.resetInstallId()
                scope.launch(Dispatchers.IO) {
                    queue.clear()
                    // Same as iOS: after opting out, the X-Client-Id of the old consent is never used again
                    attestation.forgetClient()
                }
            }
        }

    override fun track(event: AnalyticsEvent) {
        if (!isEnabled) {
            analyticsLog("dropped ${event.name}: analytics is disabled (no consent yet, or opted out)")
            return
        }

        // The identity is minted here, not at upload time: a retried upload must carry
        // the same id, or the backend would store the same event twice.
        val queued = QueuedEvent(
            id = UUID.randomUUID().toString(),
            name = event.name,
            timestamp = clock(),
            props = event.properties
        )

        scope.launch(Dispatchers.IO) {
            queue.enqueue(queued)

            val size = queue.size()
            analyticsLog("queued ${event.name}, queue=$size")

            dueFlushReason(size)?.let { flush(it) }
        }
    }

    override suspend fun flush(reason: AnalyticsFlushReason) {
        if (!isEnabled) {
            analyticsLog("flush skipped: analytics is disabled")
            return
        }

        // One flush at a time; a flush already running sends whatever this one would have
        if (!flushMutex.tryLock()) {
            analyticsLog("flush ($reason) skipped: another flush is running")
            return
        }

        try {
            lastFlushAt = clock()
            analyticsLog("flush: $reason")

            val maxBatches = if (reason == AnalyticsFlushReason.BACKGROUND) MAX_BATCHES_IN_BACKGROUND else MAX_BATCHES_PER_FLUSH
            uploader.flush(maxBatches)
        } finally {
            flushMutex.unlock()
        }
    }

    private fun dueFlushReason(queuedCount: Int): AnalyticsFlushReason? {
        val sinceLastFlush = clock() - lastFlushAt

        return when {
            queuedCount >= flushThreshold && sinceLastFlush >= FLUSH_THRESHOLD_MIN_INTERVAL_MILLIS -> AnalyticsFlushReason.THRESHOLD
            sinceLastFlush >= FLUSH_INTERVAL_MILLIS -> AnalyticsFlushReason.INTERVAL
            else -> null
        }
    }
}
