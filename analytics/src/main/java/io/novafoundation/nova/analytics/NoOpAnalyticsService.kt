package io.novafoundation.nova.analytics

class NoOpAnalyticsService : AnalyticsService {

    override var isEnabled: Boolean = false

    override fun track(event: AnalyticsEvent) = Unit

    override suspend fun flush() = Unit
}
