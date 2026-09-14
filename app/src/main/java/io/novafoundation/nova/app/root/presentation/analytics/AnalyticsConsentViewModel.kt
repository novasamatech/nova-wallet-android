package io.novafoundation.nova.app.root.presentation.analytics

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.analytics.AnalyticsOptOutManager
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.common.utils.Event
import kotlinx.coroutines.flow.MutableStateFlow

class AnalyticsConsentViewModel(
    private val analyticsOptOutManager: AnalyticsOptOutManager,
    private val appLinksProvider: AppLinksProvider,
    private val router: RootRouter,
    private val next: DelayedNavigation
) : BaseViewModel(),
    Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val disclosureExpanded = MutableStateFlow(true)

    fun agreeClicked() {
        analyticsOptOutManager.setAnalyticsEnabled(true)

        finishWithChoiceMade()
    }

    fun declineClicked() {
        finishWithChoiceMade()
    }

    fun disclosureClicked() {
        disclosureExpanded.value = !disclosureExpanded.value
    }

    fun privacyNoticeClicked() {
        openBrowserEvent.value = Event(appLinksProvider.privacyUrl)
    }

    /**
     * Either answer counts as having been asked - the screen is shown once, and declining must
     * not leave it queued up for the next launch.
     */
    private fun finishWithChoiceMade() {
        analyticsOptOutManager.setAnalyticsPromptSeen()

        router.finishAnalyticsConsent(next)
    }
}
