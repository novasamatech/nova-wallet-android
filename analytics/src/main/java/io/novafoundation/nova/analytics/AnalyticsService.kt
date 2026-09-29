package io.novafoundation.nova.analytics

interface AnalyticsService {

    var isEnabled: Boolean

    fun track(event: AnalyticsEvent)

    suspend fun flush(reason: AnalyticsFlushReason)
}

enum class AnalyticsFlushReason {
    THRESHOLD, INTERVAL, LAUNCH, BACKGROUND
}
