package io.novafoundation.nova.feature_assets.data.repository.assetFilters

import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

private const val PREF_DUST_FILTER_ENABLED = "PREF_DUST_FILTER_ENABLED"
private const val PREF_DUST_FILTER_THRESHOLD = "PREF_DUST_FILTER_THRESHOLD"
private const val PREF_DUST_FILTER_EXEMPT_TOKENS = "PREF_DUST_FILTER_EXEMPT_TOKENS"

private const val DEFAULT_THRESHOLD = "1"

class DustFilterPreferences(
    private val preferences: Preferences
) {

    fun isDustFilterEnabled(): Boolean {
        return preferences.getBoolean(PREF_DUST_FILTER_ENABLED, false)
    }

    fun setDustFilterEnabled(enabled: Boolean) {
        preferences.putBoolean(PREF_DUST_FILTER_ENABLED, enabled)
    }

    fun dustFilterEnabledFlow(): Flow<Boolean> {
        return preferences.booleanFlow(PREF_DUST_FILTER_ENABLED, false)
    }

    fun getThreshold(): BigDecimal {
        return BigDecimal(preferences.getString(PREF_DUST_FILTER_THRESHOLD, DEFAULT_THRESHOLD))
    }

    fun setThreshold(threshold: BigDecimal) {
        preferences.putString(PREF_DUST_FILTER_THRESHOLD, threshold.toPlainString())
    }

    fun thresholdFlow(): Flow<BigDecimal> {
        return preferences.stringFlow(PREF_DUST_FILTER_THRESHOLD) { DEFAULT_THRESHOLD }
            .map { BigDecimal(it ?: DEFAULT_THRESHOLD) }
    }

    fun getExemptTokens(): Set<String> {
        return preferences.getStringSet(PREF_DUST_FILTER_EXEMPT_TOKENS)
    }

    fun setExemptTokens(tokens: Set<String>) {
        preferences.putStringSet(PREF_DUST_FILTER_EXEMPT_TOKENS, tokens)
    }

    fun exemptTokensFlow(): Flow<Set<String>> {
        return preferences.stringSetFlow(PREF_DUST_FILTER_EXEMPT_TOKENS)
            .map { it ?: emptySet() }
    }

    fun dustFilterSettingsFlow(): Flow<DustFilterSettings> {
        return combine(
            dustFilterEnabledFlow(),
            thresholdFlow(),
            exemptTokensFlow()
        ) { enabled, threshold, exempt ->
            DustFilterSettings(enabled, threshold, exempt)
        }
    }
}

data class DustFilterSettings(
    val enabled: Boolean,
    val threshold: BigDecimal,
    val exemptTokenIds: Set<String>
)
