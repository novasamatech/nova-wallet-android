package io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.impl

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.api.PhishingWarningMixin

class PhishingWarningProvider(
    private val interactor: WalletInteractor
) : PhishingWarningMixin {

    private val _showPhishingWarningEvent = MutableLiveData<Event<String>>()

    override val showPhishingWarning: LiveData<Event<String>>
        get() = _showPhishingWarningEvent

    override suspend fun proceedOrShowPhishingWarning(address: String, onProceed: () -> Unit) {
        val phishingAddress = interactor.isAddressFromPhishingList(address)

        if (phishingAddress) {
            _showPhishingWarningEvent.value = Event(address)
        } else {
            onProceed()
        }
    }
}
