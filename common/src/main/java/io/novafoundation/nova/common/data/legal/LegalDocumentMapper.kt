package io.novafoundation.nova.common.data.legal

fun mapLegalDocumentsFromRemote(remote: LegalDocumentsRemote): List<LegalDocument> {
    return listOf(
        mapLegalDocumentFromRemote(remote.termsOfService, LegalDocumentType.TERMS_OF_SERVICE),
        mapLegalDocumentFromRemote(remote.privacyNotice, LegalDocumentType.PRIVACY_NOTICE)
    )
}

private fun mapLegalDocumentFromRemote(
    remote: LegalDocumentRemote,
    type: LegalDocumentType
): LegalDocument {
    return LegalDocument(
        type = type,
        version = remote.version,
        updatedAt = remote.updatedAt
    )
}
