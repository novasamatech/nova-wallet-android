package io.novafoundation.nova.feature_governance_impl.data

import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DelegationBannerService(
    private val preferences: Preferences
) {

    companion object {
        private const val PREFS_SHOULD_SHOW_BANNER = "PREFS_SHOULD_SHOW_BANNER"
    }

    private val shouldShowBannerFlow = MutableStateFlow(preferences.getBoolean(PREFS_SHOULD_SHOW_BANNER, true))

    fun shouldShowBannerFlow(): Flow<Boolean> {
        return shouldShowBannerFlow
    }

    fun hideBanner() {
        shouldShowBannerFlow.value = false
        preferences.putBoolean(PREFS_SHOULD_SHOW_BANNER, false)
    }
}
