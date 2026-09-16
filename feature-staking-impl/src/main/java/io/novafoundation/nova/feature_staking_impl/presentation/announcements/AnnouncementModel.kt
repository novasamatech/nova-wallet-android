package io.novafoundation.nova.feature_staking_impl.presentation.announcements

import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.view.AlertView

data class AnnouncementModel(
    val stylePreset: AlertView.StylePreset,
    val description: String,
    val link: Link? = null
) {

    data class Link(
        val title: String,
        val url: String
    )
}

fun mapAnnouncementToUi(announcement: Announcement): AnnouncementModel {
    return AnnouncementModel(
        stylePreset = mapAnnouncementStyleToUi(announcement.style),
        description = announcement.description,
        link = announcement.link?.let { AnnouncementModel.Link(title = it.title, url = it.url) }
    )
}

/**
 * Binds message, style and the optional "Learn more" action row. [onLinkClicked] receives the link url.
 */
fun AlertView.setAnnouncement(model: AnnouncementModel, onLinkClicked: (String) -> Unit) {
    setStylePreset(model.stylePreset)
    setMessage(model.description)
    setActionText(model.link?.title)
    setOnLinkClickedListener { model.link?.let { onLinkClicked(it.url) } }
}

private fun mapAnnouncementStyleToUi(style: Announcement.Style): AlertView.StylePreset {
    return when (style) {
        Announcement.Style.INFO -> AlertView.StylePreset.INFO
        Announcement.Style.WARNING -> AlertView.StylePreset.WARNING
        Announcement.Style.ERROR -> AlertView.StylePreset.ERROR
    }
}
