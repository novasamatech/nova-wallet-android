package io.novafoundation.nova.common.data.announcements

import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.domain.announcements.AnnouncementSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementMapperTest {

    @Test
    fun `uses translation matching the language`() {
        assertEquals("Награды возобновятся", map("ru").single().description)
    }

    @Test
    fun `falls back to default when the language is missing`() {
        assertEquals("Rewards will resume", map("fr").single().description)
    }

    @Test
    fun `default is used for english`() {
        assertEquals("Rewards will resume", map("en").single().description)
    }

    @Test
    fun `maps style ignoring case`() {
        assertEquals(Announcement.Style.WARNING, map("en").single().style)
        assertEquals(Announcement.Style.ERROR, map("en", style = "ERROR").single().style)
    }

    @Test
    fun `unknown or absent style falls back to info`() {
        assertEquals(Announcement.Style.INFO, map("en", style = null).single().style)
        assertEquals(Announcement.Style.INFO, map("en", style = "shiny").single().style)
    }

    @Test
    fun `chain id is carried over and marks a per-chain announcement`() {
        assertEquals(null, map("en").single().chainId)
        assertEquals("0xabc", map("en", chainId = "0xabc").single().chainId)
    }

    @Test
    fun `announcement without a usable translation is skipped`() {
        val mapped = mapStaking(listOf(AnnouncementRemote(null, null, mapOf("ru" to "Только русский"))), "en")

        assertTrue(mapped.isEmpty())
    }

    @Test
    fun `announcement without description is skipped instead of crashing`() {
        val mapped = mapStaking(listOf(AnnouncementRemote(null, "info", description = null)), "en")

        assertTrue(mapped.isEmpty())
    }

    @Test
    fun `null entry in the section is skipped instead of crashing`() {
        val withNullEntry = listOf(null, AnnouncementRemote(null, "info", mapOf("default" to "Survives")))

        @Suppress("UNCHECKED_CAST")
        val mapped = mapStaking(withNullEntry as List<AnnouncementRemote>, "en")

        assertEquals(listOf("Survives"), mapped.map { it.description })
    }

    @Test
    fun `absent section maps to nothing`() {
        val mapped = mapAnnouncementsFromRemote(emptyMap(), AnnouncementSection.STAKING, "en")

        assertTrue(mapped.isEmpty())
    }

    @Test
    fun `every announcement of a section is mapped`() {
        val mapped = mapStaking(
            listOf(
                AnnouncementRemote(null, "info", mapOf("default" to "First")),
                AnnouncementRemote(null, "error", mapOf("default" to "Second"))
            ),
            "en"
        )

        assertEquals(listOf("First", "Second"), mapped.map { it.description })
    }

    private fun map(languageCode: String, style: String? = "warning", chainId: String? = null): List<Announcement> {
        val remote = AnnouncementRemote(
            chainId = chainId,
            style = style,
            description = mapOf("default" to "Rewards will resume", "ru" to "Награды возобновятся")
        )

        return mapStaking(listOf(remote), languageCode)
    }

    private fun mapStaking(announcements: List<AnnouncementRemote>, languageCode: String): List<Announcement> {
        return mapAnnouncementsFromRemote(mapOf("staking" to announcements), AnnouncementSection.STAKING, languageCode)
    }
}
