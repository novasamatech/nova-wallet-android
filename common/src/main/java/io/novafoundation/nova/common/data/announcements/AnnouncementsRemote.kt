package io.novafoundation.nova.common.data.announcements

typealias AnnouncementsRemote = Map<String, List<AnnouncementRemote>>

class AnnouncementRemote(
    val style: String?,
    val description: Map<String, String>
)

private const val DEFAULT_LANGUAGE_KEY = "default"

fun Map<String, String>.localizedOrDefault(languageCode: String): String? {
    return get(languageCode) ?: get(DEFAULT_LANGUAGE_KEY)
}
