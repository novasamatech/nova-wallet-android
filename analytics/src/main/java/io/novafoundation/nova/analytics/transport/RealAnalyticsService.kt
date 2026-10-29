package io.novafoundation.nova.analytics.transport

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsFlushReason
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.analyticsLog
import io.novafoundation.nova.analytics.analyticsWarn
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val flushThreshold: Int,
    private val clock: () -> Long = System::currentTimeMillis,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AnalyticsService {

    private val flushMutex = Mutex()

    private val queueMutex = Mutex()

    private val consentLock = Any()

    private var session: ConsentSession? = null

    private var erasure: ConsentErasure? = null

    @Volatile
    private var lastFlushAt = 0L

    override var isEnabled: Boolean
        get() = synchronized(consentLock) { session != null }
        set(value) {
            if (value) {
                enableAnalytics()
            } else {
                disableAnalytics()
            }
        }

    override fun track(event: AnalyticsEvent) {
        val consent = synchronized(consentLock) { session } ?: return

        // The identity is minted here, not at upload time: a retried upload must carry
        // the same id, or the backend would store the same event twice.
        val queued = QueuedEvent(
            id = UUID.randomUUID().toString(),
            name = event.name,
            timestamp = clock(),
            props = event.properties
        )

        scope.launch(dispatcher) {
            withConsent(consent) {
                val size = queueMutex.withLock {
                    consent.job.ensureActive()
                    queue.enqueue(queued)
                    queue.size()
                }
                dueFlushReason(size)?.let { flushWithConsent(consent, it) }
            }
        }
    }

    override suspend fun flush(reason: AnalyticsFlushReason) {
        val consent = synchronized(consentLock) { session } ?: return
        withConsent(consent) {
            flushWithConsent(consent, reason)
        }
    }

    private fun enableAnalytics() = synchronized(consentLock) {
        if (session == null) {
            val job = Job()
            session = ConsentSession(job, erasure)
        }
    }

    private fun disableAnalytics() = synchronized(consentLock) {
        session?.job?.cancel()
        session = null

        // A later consent waits for cleanup. Shared authentication is never reset here.
        val pendingErasure = ConsentErasure()
        erasure = pendingErasure
        scope.launch(dispatcher) { clearWithdrawnAnalytics(pendingErasure) }
    }

    private suspend fun flushWithConsent(consent: ConsentSession, reason: AnalyticsFlushReason) {
        // One flush at a time; a flush already running sends whatever this one would have
        if (!flushMutex.tryLock()) {
            analyticsLog("flush ($reason) skipped: another flush is running")
            return
        }

        try {
            lastFlushAt = clock()
            analyticsLog("flush: $reason")

            val maxBatches = if (reason == AnalyticsFlushReason.BACKGROUND) MAX_BATCHES_IN_BACKGROUND else MAX_BATCHES_PER_FLUSH
            queueMutex.withLock {
                consent.job.ensureActive()
                uploader.flush(maxBatches)
            }
        } finally {
            flushMutex.unlock()
        }
    }

    private suspend fun withConsent(consent: ConsentSession, action: suspend () -> Unit) = coroutineScope {
        val work = async(start = CoroutineStart.LAZY) {
            consent.job.ensureActive()
            val pendingErasure = consent.erasure
            if (pendingErasure != null && clearWithdrawnAnalytics(pendingErasure) != ErasureStatus.COMPLETE) return@async
            consent.job.ensureActive()
            action()
        }
        // Cancelling this work cancels Retrofit's in-flight Call without disabling shared auth.
        val cancellation = consent.job.invokeOnCompletion { work.cancel() }
        try {
            work.await()
        } finally {
            cancellation.dispose()
        }
    }

    private suspend fun clearWithdrawnAnalytics(pendingErasure: ConsentErasure): ErasureStatus = queueMutex.withLock {
        // Delayed work from an older withdrawal must not clear a newer session's events.
        if (synchronized(consentLock) { erasure !== pendingErasure }) return@withLock ErasureStatus.RETRY_REQUIRED
        if (pendingErasure.status == ErasureStatus.COMPLETE) return@withLock ErasureStatus.COMPLETE

        var operation = "clearing queued events"
        try {
            currentCoroutineContext().ensureActive()
            queue.clear()
            currentCoroutineContext().ensureActive()
            operation = "rotating install identity"
            identity.resetInstallId()
            pendingErasure.status = ErasureStatus.COMPLETE
            ErasureStatus.COMPLETE
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            analyticsWarn(
                "consent erasure failed while $operation (${failure.javaClass.simpleName}); " +
                    "analytics blocked until the next track, flush or disable retries"
            )
            ErasureStatus.RETRY_REQUIRED
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

    private class ConsentSession(val job: Job, val erasure: ConsentErasure?)

    // Accessed only under queueMutex; completion opens the gate for this consent generation.
    private class ConsentErasure(var status: ErasureStatus = ErasureStatus.RETRY_REQUIRED)

    private enum class ErasureStatus {
        RETRY_REQUIRED,
        COMPLETE
    }
}
