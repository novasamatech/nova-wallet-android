package io.novafoundation.nova.analytics

import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface AnalyticsOptOutManager {

    val isAnalyticsEnabled: Boolean

    fun setAnalyticsEnabled(enabled: Boolean)

    fun toggleAnalytics()

    fun observeAnalyticsEnabled(): Flow<Boolean>

    fun hasSeenAnalyticsPrompt(): Boolean

    fun setAnalyticsPromptSeen()
}

class RealAnalyticsOptOutManager(
    private val preferences: Preferences,
    private val analyticsService: AnalyticsService
) : AnalyticsOptOutManager {

    companion object {
        private const val PREF_ANALYTICS_ENABLED = "analytics_enabled"
        private const val DEFAULT_ANALYTICS_ENABLED = false
        private const val PREF_HAS_SEEN_ANALYTICS_PROMPT = "hasSeenAnalyticsPrompt"
    }

    private val analyticsEnabledFlow = MutableStateFlow(getPersistedState())

    init {
        // Sync initial state from persisted settings
        analyticsService.isEnabled = getPersistedState()
    }

    override val isAnalyticsEnabled: Boolean
        get() = analyticsEnabledFlow.value

    override fun setAnalyticsEnabled(enabled: Boolean) {
        preferences.putBoolean(PREF_ANALYTICS_ENABLED, enabled)
        analyticsService.isEnabled = enabled
        analyticsEnabledFlow.value = enabled
    }

    override fun toggleAnalytics() {
        setAnalyticsEnabled(!isAnalyticsEnabled)
    }

    override fun observeAnalyticsEnabled(): Flow<Boolean> {
        return analyticsEnabledFlow
    }

    override fun hasSeenAnalyticsPrompt(): Boolean {
        return preferences.getBoolean(PREF_HAS_SEEN_ANALYTICS_PROMPT, false)
    }

    override fun setAnalyticsPromptSeen() {
        preferences.putBoolean(PREF_HAS_SEEN_ANALYTICS_PROMPT, true)
    }

    private fun getPersistedState(): Boolean {
        return preferences.getBoolean(PREF_ANALYTICS_ENABLED, DEFAULT_ANALYTICS_ENABLED)
    }
}
