package io.novafoundation.nova.analytics.transport

import androidx.annotation.Keep

const val ANALYTICS_SCHEMA_VERSION = 1

@Keep
class AnalyticsEventRequest(
    val name: String,
    val ts: String,
    val props: Map<String, Any?>
)

@Keep
class AnalyticsEventsRequest(
    val v: Int,
    val platform: String,
    val app_version: String,
    val install_id: String,
    val session_id: String,
    val sent_at: String,
    val events: List<AnalyticsEventRequest>
)
