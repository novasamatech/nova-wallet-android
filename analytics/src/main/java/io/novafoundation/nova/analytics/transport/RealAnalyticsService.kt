package io.novafoundation.nova.analytics.transport

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RealAnalyticsService(
    private val scope: CoroutineScope,
    private val queue: AnalyticsEventQueue,
    private val uploader: AnalyticsUploader,
    private val identity: AnalyticsIdentity,
    private val flushThreshold: Int
) : AnalyticsService {

    private val flushMutex = Mutex()

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

        // The identity is minted here, not at upload time: a retried upload must carry
        // the same id, or the backend would store the same event twice.
        val queued = QueuedEvent(
            id = UUID.randomUUID().toString(),
            name = event.name,
            timestamp = System.currentTimeMillis(),
            props = event.properties
        )

        scope.launch(Dispatchers.IO) {
            queue.enqueue(queued)

            // A full batch does not wait for the next tick: it is already a whole request.
            if (queue.size() >= flushThreshold) flush()
        }
    }

    override suspend fun flush() {
        if (!isEnabled) return

        flushMutex.withLock {
            uploader.flush()
        }
    }
}
