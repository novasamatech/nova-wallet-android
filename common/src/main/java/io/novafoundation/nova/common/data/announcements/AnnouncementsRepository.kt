package io.novafoundation.nova.common.data.announcements

import android.util.Log
import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.domain.announcements.AnnouncementSection
import io.novafoundation.nova.common.resources.ContextManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

private const val LOG_TAG = "AnnouncementsRepository"

interface AnnouncementsRepository {

    fun announcementsFlow(section: AnnouncementSection): Flow<List<Announcement>>
}

class RealAnnouncementsRepository(
    private val announcementsApi: AnnouncementsApi,
    private val contextManager: ContextManager
) : AnnouncementsRepository {

    @Volatile
    private var cache: AnnouncementsRemote? = null

    override fun announcementsFlow(section: AnnouncementSection): Flow<List<Announcement>> = flow {
        val cached = cache
        if (cached != null) emit(mapSection(cached, section))

        val fresh = runCatching { announcementsApi.getAnnouncements() }
            .onFailure { Log.e(LOG_TAG, "Failed to load announcements", it) }
            .getOrNull()

        when {
            fresh != null -> {
                cache = fresh
                emit(mapSection(fresh, section))
            }

            cached == null -> emit(emptyList())
        }
    }.distinctUntilChanged()

    private fun mapSection(remote: AnnouncementsRemote, section: AnnouncementSection): List<Announcement> {
        return mapAnnouncementsFromRemote(remote, section, contextManager.getLocale().language)
    }
}
