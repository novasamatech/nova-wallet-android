package io.novafoundation.nova.feature_versions_impl.data

import io.novafoundation.nova.feature_versions_impl.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Path

interface VersionsFetcher {

    @GET(BuildConfig.NOTIFICATIONS_URL)
    suspend fun getVersions(): List<VersionResponse>

    @GET(BuildConfig.NOTIFICATION_DETAILS_URL + "{version}.md")
    suspend fun getChangelog(@Path("version") version: String): String
}
