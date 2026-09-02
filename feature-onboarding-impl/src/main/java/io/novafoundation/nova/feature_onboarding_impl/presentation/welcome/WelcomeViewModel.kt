package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.analytics.AnalyticsEvent
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.OnboardingSource
import io.novafoundation.nova.analytics.WalletCreationMethod
import io.novafoundation.nova.analytics.WalletCreationStep
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.legal.LegalConsentRepository
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class WelcomeViewModel(
    shouldShowBack: Boolean,
    private val router: OnboardingRouter,
    private val appLinksProvider: AppLinksProvider,
    private val addAccountPayload: AddAccountPayload,
    private val legalConsentRepository: LegalConsentRepository,
    private val accountRepository: AccountRepository,
    private val analyticsService: AnalyticsService,
    updateNotificationsInteractor: UpdateNotificationsInteractor
) : BaseViewModel(),
    Browserable {

    val shouldShowBackLiveData: LiveData<Boolean> = MutableLiveData(shouldShowBack)

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val consentAccepted = MutableStateFlow(false)

    /**
     * Set once the user navigates to the next step of the flow so that leaving the screen afterwards is not reported as abandoning
     */
    private var proceededToNextStep = false

    private var onboardingStartedTracked = false

    val canProceed: Flow<Boolean> = consentAccepted.shareInBackground()

    init {
        updateNotificationsInteractor.allowInAppUpdateCheck()
    }

    fun consentCheckChanged(checked: Boolean) {
        consentAccepted.value = checked
        trackOnboardingStarted()
    }

    fun createAccountClicked() {
        acceptLegalDocuments()

        proceededToNextStep = true

        when (addAccountPayload) {
            is AddAccountPayload.MetaAccount -> {
                analyticsService.track(AnalyticsEvent.WalletImportMethodSelected(WalletCreationMethod.CREATE))
                router.openCreateFirstWallet()
            }

            is AddAccountPayload.ChainAccount -> router.openMnemonicScreen(accountName = null, addAccountPayload)
        }

        trackOnboardingStarted()
    }

    fun importAccountClicked() {
        acceptLegalDocuments()

        proceededToNextStep = true

        router.openImportOptionsScreen()
    }

    fun termsClicked() {
        openBrowserEvent.value = Event(appLinksProvider.termsUrl)
    }

    fun privacyClicked() {
        openBrowserEvent.value = Event(appLinksProvider.privacyUrl)
    }

    fun backClicked() {
        router.back()
    }

    override fun onCleared() {
        trackWalletCreationAbandonedIfNeeded()

        super.onCleared()
    }

    /**
     * Proceeding from this screen counts as accepting the current documents - either explicitly via the
     * checkbox, or implicitly as stated by the disclaimer under the buttons
     */
    private fun acceptLegalDocuments() {
        legalConsentRepository.acceptCurrentVersions()
    }

    private fun trackWalletCreationAbandonedIfNeeded() {
        if (proceededToNextStep) return
        if (addAccountPayload !is AddAccountPayload.MetaAccount) return

        analyticsService.track(AnalyticsEvent.WalletCreationAbandoned(WalletCreationStep.WELCOME))
    }

    private fun trackOnboardingStarted() {
        if (addAccountPayload !is AddAccountPayload.MetaAccount) return

        // Entering the flow happens once. Without this, every toggle of the consent
        // checkbox would report a fresh onboarding start.
        if (onboardingStartedTracked) return
        onboardingStartedTracked = true

        launch {
            val source = if (accountRepository.hasActiveMetaAccounts()) OnboardingSource.ADD_WALLET else OnboardingSource.FRESH_INSTALL

            analyticsService.track(AnalyticsEvent.OnboardingStarted(source))
        }
    }
}
