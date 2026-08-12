package io.novafoundation.nova.common.data.legal

import com.google.gson.Gson
import com.google.gson.JsonParseException
import org.junit.Assert.assertEquals
import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Test

class LegalDocumentsRemoteTest {

    private val gson = Gson()

    @Test
    fun `should deserialize updatedAt as a date`() {
        val remote = gson.fromJson(
            """
            {
              "termsOfService": { "version": 2, "updatedAt": "2026-08-04" },
              "privacyNotice": { "version": 1, "updatedAt": "2026-03-04" }
            }
            """.trimIndent(),
            LegalDocumentsRemote::class.java
        )

        assertEquals(2, remote.termsOfService.version)
        assertEquals(date("2026-08-04"), remote.termsOfService.updatedAt)

        assertEquals(1, remote.privacyNotice.version)
        assertEquals(date("2026-03-04"), remote.privacyNotice.updatedAt)
    }

    @Test
    fun `should map remote documents preserving the parsed date`() {
        val remote = LegalDocumentsRemote(
            termsOfService = LegalDocumentRemote(version = 3, updatedAt = date("2026-08-04")),
            privacyNotice = LegalDocumentRemote(version = 1, updatedAt = date("2026-03-04"))
        )

        val documents = mapLegalDocumentsFromRemote(remote).associateBy { it.type }

        val terms = documents.getValue(LegalDocumentType.TERMS_OF_SERVICE)
        assertEquals(3, terms.version)
        assertEquals(date("2026-08-04"), terms.updatedAt)

        val privacy = documents.getValue(LegalDocumentType.PRIVACY_NOTICE)
        assertEquals(1, privacy.version)
        assertEquals(date("2026-03-04"), privacy.updatedAt)
    }

    @Test(expected = JsonParseException::class)
    fun `should reject a malformed date so the config is treated as unavailable`() {
        gson.fromJson(
            """
            {
              "termsOfService": { "version": 1, "updatedAt": "4 March 2026" },
              "privacyNotice": { "version": 1, "updatedAt": "2026-03-04" }
            }
            """.trimIndent(),
            LegalDocumentsRemote::class.java
        )
    }

    private fun date(isoDate: String) = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(isoDate)!!
}
