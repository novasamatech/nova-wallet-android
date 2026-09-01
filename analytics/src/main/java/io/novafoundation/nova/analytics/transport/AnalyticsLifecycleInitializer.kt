package io.novafoundation.nova.analytics.transport

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.novafoundation.nova.analytics.AnalyticsOptOutManager
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.common.interfaces.ExternalServiceInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnalyticsLifecycleInitializer(
    private val scope: CoroutineScope,
    private val analyticsService: AnalyticsService,
    private val optOutManager: AnalyticsOptOutManager
) : ExternalServiceInitializer, DefaultLifecycleObserver {

    override fun initialize() {
        analyticsService.isEnabled = optOutManager.isAnalyticsEnabled

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        scope.launch(Dispatchers.IO) { analyticsService.flush() }
    }
}
