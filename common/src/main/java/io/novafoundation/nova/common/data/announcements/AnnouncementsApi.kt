package io.novafoundation.nova.common.data.announcements

import io.novafoundation.nova.common.BuildConfig
import retrofit2.http.GET

interface AnnouncementsApi {

    @GET(BuildConfig.ANNOUNCEMENTS_URL)
    suspend fun getAnnouncements(): AnnouncementsRemote
}
