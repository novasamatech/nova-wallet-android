package io.novafoundation.nova.feature_staking_impl.data.notices.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StakingNoticeDecoderTest {

    private val gson = Gson()

    private fun loadFixture(name: String): String =
        javaClass.getResourceAsStream("/notices/$name")!!.bufferedReader().use { it.readText() }

    private fun decode(name: String, locale: String, today: LocalDate = LocalDate.of(2026, 5, 12)): List<StakingNotice> {
        val rawList = gson.fromJson(loadFixture(name), Array<StakingNoticeDto>::class.java).toList()
        return rawList.mapNotNull { it.toDomain(locale, today) }
    }

    @Test
    fun `plain string shortText is decoded for any locale`() {
        val notices = decode("notice_plain_string.json", locale = "ru")
        assertEquals(1, notices.size)
        assertEquals("Plain English short", notices[0].shortText)
        assertEquals(StakingNotice.Severity.CRITICAL, notices[0].severity)
    }

    @Test
    fun `locale map picks the user locale when present`() {
        val notices = decode("notice_locale_map.json", locale = "ru")
        assertEquals("RU short", notices[0].shortText)
        assertEquals("RU long", notices[0].longText)
    }

    @Test
    fun `locale map falls back to en when user locale absent`() {
        val notices = decode("notice_locale_map.json", locale = "ja")
        assertEquals("EN short", notices[0].shortText)
    }

    @Test
    fun `locale map missing en is dropped entirely`() {
        val notices = decode("notice_missing_en.json", locale = "ja")
        assertTrue("notice without en should be dropped", notices.isEmpty())
    }

    @Test
    fun `locale falls back from pt-BR to pt to en cascade`() {
        val notices = decode("notice_locale_map.json", locale = "pt-BR")
        assertEquals("EN short", notices[0].shortText)
    }

    @Test
    fun `past endDate is filtered out`() {
        val notices = decode("notice_past_enddate.json", locale = "en", today = LocalDate.of(2026, 5, 12))
        assertTrue("past-endDate notice should be filtered", notices.isEmpty())
    }

    @Test
    fun `future endDate is kept`() {
        val notices = decode("notice_future_enddate.json", locale = "en")
        assertEquals(1, notices.size)
        assertEquals(LocalDate.of(2099, 1, 1), notices[0].endDate)
    }

    @Test
    fun `absent endDate is kept as null`() {
        val notices = decode("notice_no_enddate.json", locale = "en")
        assertEquals(1, notices.size)
        assertNull(notices[0].endDate)
    }

    @Test
    fun `severity info decodes to INFO`() {
        val notices = decode("notice_locale_map.json", locale = "en")
        assertEquals(StakingNotice.Severity.INFO, notices[0].severity)
    }

    @Test
    fun `severity critical decodes to CRITICAL`() {
        val notices = decode("notice_plain_string.json", locale = "en")
        assertEquals(StakingNotice.Severity.CRITICAL, notices[0].severity)
    }

    @Test
    fun `chainId is preserved verbatim`() {
        val notices = decode("notice_plain_string.json", locale = "en")
        assertEquals(
            "f3c7ad88f6a80f366c4be216691411ef0622e8b809b1046ea297ef106058d4eb",
            notices[0].chainId
        )
    }
}
