package io.novafoundation.nova.app.root.presentation.legal

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.legal.LegalConsentRepository
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class LegalConsentViewModel(
    private val legalConsentRepository: LegalConsentRepository,
    private val appLinksProvider: AppLinksProvider
) : BaseViewModel(),
    Browserable {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val closeEvent = MutableLiveData<Event<Unit>>()

    private val consentAccepted = MutableStateFlow(false)

    val canProceed: Flow<Boolean> = consentAccepted

    fun consentCheckChanged(checked: Boolean) {
        consentAccepted.value = checked
    }

    fun acceptClicked() {
        if (!consentAccepted.value) return

        legalConsentRepository.acceptCurrentVersions()

        closeEvent.value = Unit.event()
    }

    fun termsClicked() {
        openBrowserEvent.value = Event(appLinksProvider.termsUrl)
    }

    fun privacyClicked() {
        openBrowserEvent.value = Event(appLinksProvider.privacyUrl)
    }
}
