package io.novafoundation.nova.common.data.announcements

import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.domain.announcements.AnnouncementSection
import io.novafoundation.nova.common.resources.ContextManager

interface AnnouncementsRepository {

    suspend fun getAnnouncements(section: AnnouncementSection): List<Announcement>
}

class RealAnnouncementsRepository(
    private val announcementsApi: AnnouncementsApi,
    private val contextManager: ContextManager
) : AnnouncementsRepository {

    override suspend fun getAnnouncements(section: AnnouncementSection): List<Announcement> {
        val remote = runCatching { announcementsApi.getAnnouncements() }.getOrNull() ?: return emptyList()

        return mapAnnouncementsFromRemote(remote, section, contextManager.getLocale().language)
    }
}
