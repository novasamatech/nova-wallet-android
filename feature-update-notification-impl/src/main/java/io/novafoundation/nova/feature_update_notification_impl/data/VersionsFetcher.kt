package io.novafoundation.nova.feature_update_notification_impl.data

import io.novafoundation.nova.feature_update_notification_impl.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Path

interface VersionsFetcher {

    @GET(BuildConfig.NOTIFICATIONS_URL)
    suspend fun getVersions(): List<VersionResponse>

    @GET(BuildConfig.NOTIFICATION_DETAILS_URL + "{version}")
    suspend fun getVersionDetails(@Path("version") version: String): String
}
