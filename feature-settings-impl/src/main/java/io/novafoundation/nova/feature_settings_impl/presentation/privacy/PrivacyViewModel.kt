package io.novafoundation.nova.feature_settings_impl.presentation.privacy

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.analytics.AnalyticsOptOutManager
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.SettingsRouter

class PrivacyViewModel(
    private val analyticsOptOutManager: AnalyticsOptOutManager,
    private val appLinksProvider: AppLinksProvider,
    private val resourceManager: ResourceManager,
    private val router: SettingsRouter
) : BaseViewModel(),
    Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val analyticsEnabledState = analyticsOptOutManager.observeAnalyticsEnabled()

    fun analyticsClicked() {
        val enabled = !analyticsOptOutManager.isAnalyticsEnabled

        analyticsOptOutManager.setAnalyticsEnabled(enabled)

        // Opting out needs no acknowledgement, the switch itself shows the result
        if (enabled) {
            showToast(resourceManager.getString(R.string.privacy_analytics_enabled_toast))
        }
    }

    fun privacyNoticeClicked() {
        openBrowserEvent.value = Event(appLinksProvider.privacyUrl)
    }

    fun backClicked() {
        router.back()
    }
}
