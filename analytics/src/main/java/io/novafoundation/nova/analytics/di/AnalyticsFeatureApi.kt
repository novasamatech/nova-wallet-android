package io.novafoundation.nova.analytics.di

import io.novafoundation.nova.analytics.AnalyticsOptOutManager
import io.novafoundation.nova.analytics.AnalyticsService

interface AnalyticsFeatureApi {

    val analyticsService: AnalyticsService

    val analyticsOptOutManager: AnalyticsOptOutManager
}
