package io.novafoundation.nova.common.data.legal

import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface LegalConsentRepository {

    suspend fun getDocuments(): List<LegalDocument>?

    suspend fun isConsentRequired(): Boolean

    fun acceptCurrentVersions()
}

class RealLegalConsentRepository(
    private val legalDocumentsApi: LegalDocumentsApi,
    private val preferences: Preferences
) : LegalConsentRepository {

    companion object {

        private const val PREFS_ACCEPTED_VERSION_PREFIX = "accepted_legal_version_"

        private const val PREFS_CONSENT_PENDING_SYNC = "legal_consent_pending_sync"

        private const val NOTHING_ACCEPTED = 0
    }

    private val mutex = Mutex()

    @Volatile
    private var documents: List<LegalDocument>? = null

    override suspend fun getDocuments(): List<LegalDocument>? {
        return syncDocuments()
    }

    override suspend fun isConsentRequired(): Boolean {
        val documents = syncDocuments() ?: return false

        return documents.any { acceptedVersion(it.type) < it.version }
    }

    override fun acceptCurrentVersions() {
        val loadedDocuments = documents

        if (loadedDocuments == null) {
            // Versions are not known yet - the first successful sync will record them as accepted
            preferences.putBoolean(PREFS_CONSENT_PENDING_SYNC, true)
        } else {
            accept(loadedDocuments)
        }
    }

    private suspend fun syncDocuments(): List<LegalDocument>? = mutex.withLock {
        documents?.let { return it }

        // A missing or malformed config must not break the app - it just means we cannot ask for consent yet
        val loaded = runCatching { mapLegalDocumentsFromRemote(legalDocumentsApi.getLegalDocuments()) }
            .getOrNull() ?: return null

        documents = loaded

        if (preferences.getBoolean(PREFS_CONSENT_PENDING_SYNC, false)) {
            accept(loaded)
        }

        return loaded
    }

    private fun accept(documents: List<LegalDocument>) {
        documents.forEach { preferences.putInt(keyOf(it.type), it.version) }

        preferences.putBoolean(PREFS_CONSENT_PENDING_SYNC, false)
    }

    private fun acceptedVersion(type: LegalDocumentType): Int {
        return preferences.getInt(keyOf(type), NOTHING_ACCEPTED)
    }

    private fun keyOf(type: LegalDocumentType) = PREFS_ACCEPTED_VERSION_PREFIX + type.name
}
