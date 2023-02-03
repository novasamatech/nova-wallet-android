package io.novafoundation.nova.feature_governance_impl.data.repository

import io.novafoundation.nova.common.data.storage.Preferences

interface RemoveVotesSuggestionRepository {

    fun isAllowedToShowRemoveVotesSuggestion(): Boolean

    suspend fun disallowShowRemoveVotesSuggestion()
}

class RealRemoveVotesSuggestionRepository(
    private val preferences: Preferences
) : RemoveVotesSuggestionRepository {

    companion object {
        private const val PREFS_KEY = "RemoveVotesSuggestionRepository.ShouldShowSuggestion"

        private const val ALLOWED_DEFAULT = true
    }

    override fun isAllowedToShowRemoveVotesSuggestion(): Boolean {
        return preferences.getBoolean(PREFS_KEY, ALLOWED_DEFAULT)
    }

    override suspend fun disallowShowRemoveVotesSuggestion() {
        preferences.putBoolean(PREFS_KEY, value = false)
    }
}
