package io.novafoundation.nova.common.data.analytics

interface AnalyticsService {

    var isEnabled: Boolean

    fun track(event: AnalyticsEvent)
}
