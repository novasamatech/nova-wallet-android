package io.novafoundation.nova.app.root.presentation.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.analytics.AnalyticsOptOutManager
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.common.data.legal.LegalConsentRepository
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_ahm_api.domain.ChainMigrationDetailsSelectToShowUseCase
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import kotlinx.coroutines.launch

class MainViewModel(
    updateNotificationsInteractor: UpdateNotificationsInteractor,
    private val automaticInteractionGate: AutomaticInteractionGate,
    private val welcomePushNotificationsInteractor: WelcomePushNotificationsInteractor,
    private val legalConsentRepository: LegalConsentRepository,
    private val accountRepository: AccountRepository,
    private val rootRouter: RootRouter,
    private val chainMigrationDetailsSelectToShowUseCase: ChainMigrationDetailsSelectToShowUseCase,
    private val analyticsService: AnalyticsService,
    private val analyticsOptOutManager: AnalyticsOptOutManager
) : BaseViewModel() {

    init {
        updateNotificationsInteractor.allowInAppUpdateCheck()
        automaticInteractionGate.initialPinPassed()

        checkLegalConsent()

        checkAnalyticsConsent()

        if (welcomePushNotificationsInteractor.needToShowWelcomeScreen()) {
            rootRouter.openPushWelcome()
        }

        launch {
            val chainIdsToShowMigrationDetails = chainMigrationDetailsSelectToShowUseCase.getChainIdsToShowMigrationDetails()
            chainIdsToShowMigrationDetails.forEach {
                rootRouter.openChainMigrationDetails(it)
            }
        }
    }

    /**
     * Only users that already have a wallet are asked to accept the updated documents - someone who has just
     * onboarded has accepted them on the welcome screen already.
     */
    private val _showAnalyticsConsent = MutableLiveData<Event<Unit>>()
    val showAnalyticsConsent: LiveData<Event<Unit>> = _showAnalyticsConsent

    private var currentTab: String? = null

    fun onDestinationChanged(destinationId: Int) {
        val tab = tabNameFor(destinationId) ?: return
        if (tab == currentTab) return

        val hadPreviousTab = currentTab != null
        currentTab = tab

        if (hadPreviousTab) {
            analyticsService.track(AnalyticsEvent.TabSwitched(tab))
        }
    }

    private fun tabNameFor(destinationId: Int): String? = when (destinationId) {
        R.id.walletFragment -> "assets"
        R.id.voteFragment -> "vote"
        R.id.dAppsFragment -> "dapps"
        R.id.stakingDashboardFragment -> "staking"
        R.id.profileFragment -> "settings"
        else -> null
    }

    fun onAnalyticsConsentGiven() {
        analyticsOptOutManager.setAnalyticsEnabled(true)
        analyticsOptOutManager.setAnalyticsPromptSeen()
    }

    fun onAnalyticsConsentDeclined() {
        analyticsOptOutManager.setAnalyticsPromptSeen()
    }

    /**
     * Asked once, and only of someone who already has a wallet: a person still going
     * through onboarding should not meet this on top of the setup flow, and the legal
     * consent screen takes precedence when both are due.
     */
    private fun checkAnalyticsConsent() = launch {
        if (analyticsOptOutManager.hasSeenAnalyticsPrompt()) return@launch
        if (!accountRepository.hasActiveMetaAccounts()) return@launch
        if (legalConsentRepository.isConsentRequired()) return@launch

        _showAnalyticsConsent.value = Event(Unit)
    }

    private fun checkLegalConsent() = launch {
        if (!accountRepository.hasActiveMetaAccounts()) return@launch

        if (legalConsentRepository.isConsentRequired()) {
            rootRouter.openLegalConsent()
        }
    }
}
