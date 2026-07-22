package io.novafoundation.nova.app.root.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.analytics.AnalyticsEvent
import io.novafoundation.nova.common.data.analytics.AnalyticsOptOutManager
import io.novafoundation.nova.common.data.analytics.AnalyticsService
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_ahm_api.domain.ChainMigrationDetailsSelectToShowUseCase
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import kotlinx.coroutines.launch

class MainViewModel(
    updateNotificationsInteractor: UpdateNotificationsInteractor,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val welcomePushNotificationsInteractor: WelcomePushNotificationsInteractor,
    private val rootRouter: RootRouter,
    private val chainMigrationDetailsSelectToShowUseCase: ChainMigrationDetailsSelectToShowUseCase,
    private val analyticsOptOutManager: AnalyticsOptOutManager,
    private val analyticsService: AnalyticsService,
    private val preferences: Preferences
) : BaseViewModel() {

    companion object {
        private const val PREF_IS_FIRST_LAUNCH = "is_first_launch"
    }

    private val _showAnalyticsPromptEvent = MutableLiveData<Event<Unit>>()
    val showAnalyticsPromptEvent: LiveData<Event<Unit>> = _showAnalyticsPromptEvent

    init {
        updateNotificationsInteractor.allowInAppUpdateCheck()
        automaticInteractionGate.initialPinPassed()

        if (welcomePushNotificationsInteractor.needToShowWelcomeScreen()) {
            rootRouter.openPushWelcome()
        }

        launch {
            val chainIdsToShowMigrationDetails = chainMigrationDetailsSelectToShowUseCase.getChainIdsToShowMigrationDetails()
            chainIdsToShowMigrationDetails.forEach {
                rootRouter.openChainMigrationDetails(it)
            }
        }

        checkAnalyticsPrompt()
        trackAppOpened()
    }

    fun onAnalyticsEnabled() {
        analyticsOptOutManager.setAnalyticsEnabled(true)
        analyticsOptOutManager.setAnalyticsPromptSeen()
        // Fire app_opened now that analytics is enabled
        trackAppOpened()
    }

    fun onAnalyticsDismissed() {
        analyticsOptOutManager.setAnalyticsPromptSeen()
    }

    private fun checkAnalyticsPrompt() {
        if (!analyticsOptOutManager.hasSeenAnalyticsPrompt()) {
            _showAnalyticsPromptEvent.value = Event(Unit)
        }
    }

    fun onTabSwitched(tab: String) {
        analyticsService.track(AnalyticsEvent.TabSwitched(tab))
    }

    private fun trackAppOpened() {
        val isFirstLaunch = !preferences.contains(PREF_IS_FIRST_LAUNCH)
        if (analyticsService.isEnabled) {
            analyticsService.track(AnalyticsEvent.AppOpened(isFirstLaunch))
            if (isFirstLaunch) {
                preferences.putBoolean(PREF_IS_FIRST_LAUNCH, false)
            }
        }
    }
}
