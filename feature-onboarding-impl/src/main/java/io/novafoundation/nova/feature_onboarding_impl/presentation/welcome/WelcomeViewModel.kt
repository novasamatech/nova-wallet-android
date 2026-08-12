package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

class WelcomeViewModel(
    shouldShowBack: Boolean,
    private val router: OnboardingRouter,
    private val appLinksProvider: AppLinksProvider,
    private val addAccountPayload: AddAccountPayload,
    private val legalConsentRepository: LegalConsentRepository,
    private val accountRepository: AccountRepository,
    updateNotificationsInteractor: UpdateNotificationsInteractor
) : BaseViewModel(),
    Browserable {

    val shouldShowBackLiveData: LiveData<Boolean> = MutableLiveData(shouldShowBack)

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val consentAccepted = MutableStateFlow(false)

    val canProceed: Flow<Boolean> = consentAccepted.shareInBackground()

    init {
        updateNotificationsInteractor.allowInAppUpdateCheck()
    }

    fun consentCheckChanged(checked: Boolean) {
        consentAccepted.value = checked
    }

    fun createAccountClicked() {
        acceptLegalDocuments()

        when (addAccountPayload) {
            is AddAccountPayload.MetaAccount -> router.openCreateFirstWallet()
            is AddAccountPayload.ChainAccount -> router.openMnemonicScreen(accountName = null, addAccountPayload)
        }
    }

    fun importAccountClicked() {
        acceptLegalDocuments()

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

    /**
     * Proceeding from this screen counts as accepting the current documents - either explicitly via the
     * checkbox, or implicitly as stated by the disclaimer under the buttons
     */
    private fun acceptLegalDocuments() {
        legalConsentRepository.acceptCurrentVersions()
    }
}
