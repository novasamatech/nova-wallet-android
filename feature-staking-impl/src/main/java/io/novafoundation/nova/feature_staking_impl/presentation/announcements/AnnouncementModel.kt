package io.novafoundation.nova.feature_staking_impl.presentation.announcements

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.view.AlertView

data class AnnouncementModel(
    val stylePreset: AlertView.StylePreset,
    @ColorRes val backgroundColorRes: Int,
    @DrawableRes val iconRes: Int,
    val description: String
)

fun mapAnnouncementToUi(announcement: Announcement): AnnouncementModel {
    return AnnouncementModel(
        stylePreset = mapAnnouncementStyleToUi(announcement.style),
        backgroundColorRes = mapAnnouncementBackgroundToUi(announcement.style),
        iconRes = mapAnnouncementIconToUi(announcement.style),
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

@ColorRes
private fun mapAnnouncementBackgroundToUi(style: Announcement.Style): Int {
    return when (style) {
        Announcement.Style.INFO -> R.color.individual_chip_background
        Announcement.Style.WARNING -> R.color.warning_block_background
        Announcement.Style.ERROR -> R.color.error_block_background
    }
}

@DrawableRes
private fun mapAnnouncementIconToUi(style: Announcement.Style): Int {
    return when (style) {
        Announcement.Style.INFO -> R.drawable.ic_info_accent
        Announcement.Style.WARNING -> R.drawable.ic_warning_filled
        Announcement.Style.ERROR -> R.drawable.ic_slash
    }
}
