package io.novafoundation.nova.analytics.transport

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val FLUSH_INTERVAL_MILLIS = 5 * 60 * 1000L

class RealAnalyticsService(
    private val scope: CoroutineScope,
    private val queue: AnalyticsEventQueue,
    private val uploader: AnalyticsUploader,
    private val identity: AnalyticsIdentity,
    private val flushThreshold: Int
) : AnalyticsService {

    private val flushMutex = Mutex()

    @Volatile
    private var lastFlushAt = 0L

    @Volatile
    override var isEnabled: Boolean = false
        set(value) {
            field = value
            if (!value) {
                identity.resetInstallId()
                scope.launch(Dispatchers.IO) { queue.clear() }
            }
        }

    override fun track(event: AnalyticsEvent) {
        if (!isEnabled) return

        val queued = QueuedEvent(event.name, System.currentTimeMillis(), event.properties)

        scope.launch(Dispatchers.IO) {
            queue.enqueue(queued)

            if (shouldFlush()) flush()
        }
    }

    override suspend fun flush() {
        if (!isEnabled) return

        flushMutex.withLock {
            lastFlushAt = System.currentTimeMillis()

            uploader.flush()
        }
    }

    private suspend fun shouldFlush(): Boolean {
        if (queue.size() >= flushThreshold) return true

        return System.currentTimeMillis() - lastFlushAt >= FLUSH_INTERVAL_MILLIS
    }
}
