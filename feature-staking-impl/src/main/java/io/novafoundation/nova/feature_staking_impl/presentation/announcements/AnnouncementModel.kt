package io.novafoundation.nova.feature_staking_impl.presentation.announcements

import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.view.AlertView

data class AnnouncementModel(
    val stylePreset: AlertView.StylePreset,
    val description: String
)

fun mapAnnouncementToUi(announcement: Announcement): AnnouncementModel {
    return AnnouncementModel(
        stylePreset = mapAnnouncementStyleToUi(announcement.style),
        description = announcement.description
    )
}

private fun mapAnnouncementStyleToUi(style: Announcement.Style): AlertView.StylePreset {
    return when (style) {
        Announcement.Style.INFO -> AlertView.StylePreset.INFO
        Announcement.Style.WARNING -> AlertView.StylePreset.WARNING
        Announcement.Style.ERROR -> AlertView.StylePreset.ERROR
    }
}
