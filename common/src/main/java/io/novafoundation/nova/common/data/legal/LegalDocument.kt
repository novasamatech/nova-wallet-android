package io.novafoundation.nova.common.data.legal

import java.util.Date

enum class LegalDocumentType {
    TERMS_OF_SERVICE,
    PRIVACY_NOTICE
}

class LegalDocument(
    val type: LegalDocumentType,
    val version: Int,
    val updatedAt: Date
)
