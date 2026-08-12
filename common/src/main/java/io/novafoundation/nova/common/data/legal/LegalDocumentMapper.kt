package io.novafoundation.nova.common.data.legal

import java.text.SimpleDateFormat
import java.util.Locale

private const val UPDATED_AT_FORMAT = "yyyy-MM-dd"

fun mapLegalDocumentsFromRemote(remote: LegalDocumentsRemote): List<LegalDocument> {
    // Locale.ROOT since the remote format is fixed and locale-independent
    val dateFormat = SimpleDateFormat(UPDATED_AT_FORMAT, Locale.ROOT)

    return listOf(
        mapLegalDocumentFromRemote(remote.termsOfService, LegalDocumentType.TERMS_OF_SERVICE, dateFormat),
        mapLegalDocumentFromRemote(remote.privacyNotice, LegalDocumentType.PRIVACY_NOTICE, dateFormat)
    )
}

private fun mapLegalDocumentFromRemote(
    remote: LegalDocumentRemote,
    type: LegalDocumentType,
    dateFormat: SimpleDateFormat
): LegalDocument {
    val updatedAt = requireNotNull(dateFormat.parse(remote.updatedAt)) {
        "Unexpected updatedAt format for $type: ${remote.updatedAt}"
    }

    return LegalDocument(type = type, version = remote.version, updatedAt = updatedAt)
}
