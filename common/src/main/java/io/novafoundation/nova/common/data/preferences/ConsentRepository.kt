package io.novafoundation.nova.common.data.preferences

import io.novafoundation.nova.common.data.storage.Preferences

interface ConsentRepository {

    val currentConsentVersion: Int

    fun acceptedConsentVersion(): Int?

    fun hasAcceptedCurrentVersion(): Boolean

    fun acceptCurrentConsent()
}

internal class SharedPrefsConsentRepository(
    private val preferences: Preferences
) : ConsentRepository {

    companion object {
        private const val ACCEPTED_VERSION_KEY = "ConsentRepository.consent_accepted_version"
        private const val NO_VERSION = -1
    }

    override val currentConsentVersion: Int = ConsentBannerConstants.CURRENT_CONSENT_VERSION

    override fun acceptedConsentVersion(): Int? {
        val stored = preferences.getInt(ACCEPTED_VERSION_KEY, NO_VERSION)
        return stored.takeIf { it != NO_VERSION }
    }

    override fun hasAcceptedCurrentVersion(): Boolean {
        val accepted = acceptedConsentVersion() ?: return false
        return accepted >= currentConsentVersion
    }

    override fun acceptCurrentConsent() {
        preferences.putInt(ACCEPTED_VERSION_KEY, currentConsentVersion)
    }
}
