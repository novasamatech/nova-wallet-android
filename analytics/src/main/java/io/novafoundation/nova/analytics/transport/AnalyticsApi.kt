package io.novafoundation.nova.analytics.transport

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

const val EVENTS_PATH = "v1/analytics/events"

interface AnalyticsApi {

    // A full URL: the host comes from the global config at request time, see InfrastructureUrls
    @POST
    suspend fun sendEvents(@Url url: String, @Body body: AnalyticsEventsRequest)
}
