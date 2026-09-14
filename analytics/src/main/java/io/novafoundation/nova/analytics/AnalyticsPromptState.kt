package io.novafoundation.nova.analytics

import io.novafoundation.nova.common.data.storage.Preferences

private const val PREF_HAS_SEEN_ANALYTICS_PROMPT = "hasSeenAnalyticsPrompt"

/**
 * Whether the analytics consent screen has been answered, either way.
 *
 * Lives apart from [AnalyticsOptOutManager] because navigation has to decide right after the PIN
 * whether to show the screen, and the router is built in the app graph, before any feature graph
 * exists. This class is the only owner of the flag, so both sides read the same thing.
 */
class AnalyticsPromptState(private val preferences: Preferences) {

    fun hasBeenAnswered(): Boolean = preferences.getBoolean(PREF_HAS_SEEN_ANALYTICS_PROMPT, false)

    fun markAnswered() = preferences.putBoolean(PREF_HAS_SEEN_ANALYTICS_PROMPT, true)
}
