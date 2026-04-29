package io.novafoundation.nova.feature_onboarding_impl.presentation.consentUpgrade

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.preferences.ConsentBannerConstants
import io.novafoundation.nova.common.data.preferences.ConsentRepository
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class ConsentBannerUpgradeViewModel(
    private val router: OnboardingRouter,
    private val consentRepository: ConsentRepository,
    private val resourceManager: ResourceManager,
) : BaseViewModel(),
    Browserable {

    private val _consentAccepted = MutableStateFlow(false)
    val consentAccepted: Flow<Boolean> = _consentAccepted

    val acceptButtonState: Flow<DescriptiveButtonState> = _consentAccepted.map { accepted ->
        val acceptText = resourceManager.getString(R.string.consent_banner_upgrade_accept)
        if (accepted) {
            DescriptiveButtonState.Enabled(acceptText)
        } else {
            DescriptiveButtonState.Disabled(acceptText)
        }
    }

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    fun consentToggled(checked: Boolean) {
        _consentAccepted.value = checked
    }

    fun acceptClicked() {
        consentRepository.acceptCurrentConsent()
        router.back()
    }

    fun termsClicked() {
        openBrowserEvent.value = Event(ConsentBannerConstants.TERMS_OF_SERVICE_URL)
    }

    fun privacyClicked() {
        openBrowserEvent.value = Event(ConsentBannerConstants.PRIVACY_NOTICE_URL)
    }
}
