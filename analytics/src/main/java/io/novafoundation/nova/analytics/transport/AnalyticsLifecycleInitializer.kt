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
import kotlinx.coroutines.launch

class AnalyticsLifecycleInitializer(
    private val scope: CoroutineScope,
    private val analyticsService: AnalyticsService,
    private val optOutManager: AnalyticsOptOutManager
) : ExternalServiceInitializer, DefaultLifecycleObserver {

    private var sessionStartedAt: Long = 0L

    override fun initialize() {
        analyticsService.isEnabled = optOutManager.isAnalyticsEnabled

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        sessionStartedAt = System.currentTimeMillis()
        analyticsService.track(AnalyticsEvent.SessionStarted)
    }

    override fun onStop(owner: LifecycleOwner) {
        if (sessionStartedAt != 0L) {
            val duration = System.currentTimeMillis() - sessionStartedAt
            analyticsService.track(AnalyticsEvent.SessionEnded(DurationBucket.from(duration)))
        }

        scope.launch(Dispatchers.IO) { analyticsService.flush() }
    }
}
