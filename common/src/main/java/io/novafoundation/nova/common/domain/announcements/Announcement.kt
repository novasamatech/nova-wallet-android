package io.novafoundation.nova.common.domain.announcements

data class Announcement(
    val chainId: String?,
    val style: Style,
    val description: String,
    val link: Link? = null
) {

    enum class Style {
        INFO, WARNING, ERROR
    }

    data class Link(
        val url: String,
        val title: String
    )
}

enum class AnnouncementSection(val key: String) {
    STAKING("staking")
}
