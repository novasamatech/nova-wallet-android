package io.novafoundation.nova.feature_swap_impl.data

import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SwapChartPreferences @Inject constructor(private val preferences: Preferences) {

    fun enabled(): Flow<Boolean> = preferences.booleanFlow(ENABLED, defaultValue = true)

    fun isEnabled(): Boolean = preferences.getBoolean(ENABLED, defaultValue = true)

    fun setEnabled(enabled: Boolean) = preferences.putBoolean(ENABLED, enabled)

    fun isExpanded(): Boolean = preferences.getBoolean(EXPANDED, defaultValue = false)

    fun setExpanded(expanded: Boolean) = preferences.putBoolean(EXPANDED, expanded)

    private companion object {
        const val ENABLED = "swap.chart.enabled"
        const val EXPANDED = "swap.chart.expanded"
    }
}
