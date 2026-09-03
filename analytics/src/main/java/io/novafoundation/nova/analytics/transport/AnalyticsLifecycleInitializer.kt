package io.novafoundation.nova.analytics.transport

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsOptOutManager
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.DurationBucket
import io.novafoundation.nova.common.interfaces.ExternalServiceInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Uploads run on a timer, the same shape the backend delivery worker uses: events
 * leave on a fixed cadence instead of waiting for the queue to fill. The ticker only
 * runs while the app is in the foreground — a background app has nothing to report
 * and should not hold a wakeup — and backgrounding flushes once on the way out.
 */
private const val FLUSH_INTERVAL_MILLIS = 60 * 1000L

class AnalyticsLifecycleInitializer(
    private val scope: CoroutineScope,
    private val analyticsService: AnalyticsService,
    private val optOutManager: AnalyticsOptOutManager
) : ExternalServiceInitializer, DefaultLifecycleObserver {

    private var sessionStartedAt: Long = 0L

    private var flushTicker: Job? = null

    override fun initialize() {
        analyticsService.isEnabled = optOutManager.isAnalyticsEnabled

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        sessionStartedAt = System.currentTimeMillis()
        analyticsService.track(AnalyticsEvent.SessionStarted)

        flushTicker = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(FLUSH_INTERVAL_MILLIS)
                analyticsService.flush()
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (sessionStartedAt != 0L) {
            val duration = System.currentTimeMillis() - sessionStartedAt
            analyticsService.track(AnalyticsEvent.SessionEnded(DurationBucket.from(duration)))
        }

        flushTicker?.cancel()
        flushTicker = null

        scope.launch(Dispatchers.IO) { analyticsService.flush() }
    }
}
