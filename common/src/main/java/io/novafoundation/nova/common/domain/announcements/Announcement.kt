package io.novafoundation.nova.common.domain.announcements

class Announcement(
    val style: Style,
    val description: String
) {

    enum class Style {
        INFO, WARNING, ERROR
    }
}

enum class AnnouncementSection(val key: String) {
    STAKING("staking")
}
