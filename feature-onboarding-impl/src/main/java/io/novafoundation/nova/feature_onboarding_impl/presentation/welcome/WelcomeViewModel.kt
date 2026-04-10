package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.preferences.ConsentBannerConstants
import io.novafoundation.nova.common.data.preferences.ConsentRepository
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor

class WelcomeViewModel(
    shouldShowBack: Boolean,
    private val router: OnboardingRouter,
    private val addAccountPayload: AddAccountPayload,
    updateNotificationsInteractor: UpdateNotificationsInteractor,
    private val consentRepository: ConsentRepository
) : BaseViewModel(),
    Browserable {

    val shouldShowBackLiveData: LiveData<Boolean> = MutableLiveData(shouldShowBack)

    private val _consentAccepted = MutableLiveData(consentRepository.hasAcceptedCurrentVersion())
    val consentAccepted: LiveData<Boolean> = _consentAccepted

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    init {
        updateNotificationsInteractor.allowInAppUpdateCheck()
    }

    fun consentToggled(checked: Boolean) {
        _consentAccepted.value = checked
    }

    fun createAccountClicked() {
        // The view enforces that the consent checkbox is checked before this
        // method can be invoked. Persist the acceptance now so subsequent
        // wallet operations and the existing-user upgrade modal both observe
        // the recorded version.
        consentRepository.acceptCurrentConsent()
        when (addAccountPayload) {
            is AddAccountPayload.MetaAccount -> router.openCreateFirstWallet()
            is AddAccountPayload.ChainAccount -> router.openMnemonicScreen(accountName = null, addAccountPayload)
        }
    }

    fun importAccountClicked() {
        consentRepository.acceptCurrentConsent()
        router.openImportOptionsScreen()
    }

    fun termsClicked() {
        // The consent banner uses dedicated novasama.io URLs (placeholder until
        // the canonical pages land — see ConsentBannerConstants). The legacy
        // appLinksProvider.termsUrl still points at the OLD novawallet.io
        // document and must NOT be reused here.
        openBrowserEvent.value = Event(ConsentBannerConstants.TERMS_OF_SERVICE_URL)
    }

    fun privacyClicked() {
        openBrowserEvent.value = Event(ConsentBannerConstants.PRIVACY_NOTICE_URL)
    }

    fun backClicked() {
        router.back()
    }
}
