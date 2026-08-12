package io.novafoundation.nova.common.data.legal


class LegalDocumentsRemote(
    val termsOfService: LegalDocumentRemote,
    val privacyNotice: LegalDocumentRemote
)

class LegalDocumentRemote(
    val version: Int,
    /**
     * ISO-8601 date, e.g. `2026-03-04`
     */
    val updatedAt: String
)
