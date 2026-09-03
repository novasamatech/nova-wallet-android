package io.novafoundation.nova.analytics.transport

import retrofit2.http.Body
import retrofit2.http.POST

interface AnalyticsApi {

    @POST("v1/analytics/events")
    suspend fun sendEvents(@Body body: AnalyticsEventsRequest)
}
