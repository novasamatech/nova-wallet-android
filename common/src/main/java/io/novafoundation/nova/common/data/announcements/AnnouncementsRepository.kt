package io.novafoundation.nova.common.data.announcements

import android.util.Log
import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.domain.announcements.AnnouncementSection
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.coroutines.DangerousScope
import io.novafoundation.nova.common.utils.coroutines.RootScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

private const val LOG_TAG = "AnnouncementsRepository"

interface AnnouncementsRepository {

    fun announcementsFlow(section: AnnouncementSection): Flow<List<Announcement>>
}

@OptIn(DangerousScope::class)
class RealAnnouncementsRepository(
    private val announcementsApi: AnnouncementsApi,
    private val contextManager: ContextManager,
    rootScope: RootScope,
) : AnnouncementsRepository {

    @Volatile
    private var cache: AnnouncementsRemote? = null

    private val remoteAnnouncements = flow {
        val cached = cache
        if (cached != null) emit(cached)

        val fresh = runCatching { announcementsApi.getAnnouncements() }
            .onFailure { Log.e(LOG_TAG, "Failed to load announcements", it) }
            .getOrNull()

        when {
            fresh != null -> {
                cache = fresh
                emit(fresh)
            }

            cached == null -> emit(emptyMap())
        }
    }.shareIn(rootScope, SharingStarted.WhileSubscribed(), replay = 1)

    override fun announcementsFlow(section: AnnouncementSection): Flow<List<Announcement>> {
        return remoteAnnouncements
            .map { mapAnnouncementsFromRemote(it, section, contextManager.getLocale().language) }
            .catch {
                Log.e(LOG_TAG, "Failed to map announcements", it)
                emit(emptyList())
            }
            .distinctUntilChanged()
    }
}
