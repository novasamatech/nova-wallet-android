package io.novafoundation.nova.feature_swap_impl.data

import io.novafoundation.nova.common.data.storage.Preferences
import javax.inject.Inject

class SwapChartPreferences @Inject constructor(private val preferences: Preferences) {

    fun isExpanded(): Boolean = preferences.getBoolean(EXPANDED, defaultValue = false)

    fun setExpanded(expanded: Boolean) = preferences.putBoolean(EXPANDED, expanded)

    private companion object {
        const val EXPANDED = "swap.chart.expanded"
    }
}
