package io.novafoundation.nova.common.data.announcements

import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.domain.announcements.AnnouncementSection

fun mapAnnouncementsFromRemote(
    remote: AnnouncementsRemote,
    section: AnnouncementSection,
    languageCode: String
): List<Announcement> {
    return remote[section.key].orEmpty().mapNotNull { mapAnnouncementFromRemote(it, languageCode) }
}

private fun mapAnnouncementFromRemote(remote: AnnouncementRemote?, languageCode: String): Announcement? {
    val description = remote?.description?.localizedOrDefault(languageCode) ?: return null

    return Announcement(
        chainId = remote.chainId,
        style = mapStyleFromRemote(remote.style),
        description = description
    )
}

private fun mapStyleFromRemote(style: String?): Announcement.Style {
    return Announcement.Style.entries.find { it.name.equals(style, ignoreCase = true) }
        ?: Announcement.Style.INFO
}
