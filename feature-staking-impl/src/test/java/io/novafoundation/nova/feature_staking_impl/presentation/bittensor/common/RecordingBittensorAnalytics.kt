package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsFlushReason
import io.novafoundation.nova.analytics.AnalyticsService

class RecordingBittensorAnalytics : AnalyticsService {
    override var isEnabled = true
    val events = mutableListOf<AnalyticsEvent>()
    override fun track(event: AnalyticsEvent) {
        if (isEnabled) events += event
    }
    override suspend fun flush(reason: AnalyticsFlushReason) = Unit
}
