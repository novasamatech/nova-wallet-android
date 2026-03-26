package io.novafoundation.nova.common.data.analytics

class NoOpAnalyticsService : AnalyticsService {

    override var isEnabled: Boolean = false

    override fun track(event: AnalyticsEvent) {
        // No-op for tests
    }
}
