package io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.impl

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_impl.domain.send.SendInteractor
import io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.api.PhishingWarningMixin

class PhishingWarningProvider(
    private val sendInteractor: SendInteractor
) : PhishingWarningMixin {

    private val _showPhishingWarningEvent = MutableLiveData<Event<String>>()

    override val showPhishingWarning: LiveData<Event<String>>
        get() = _showPhishingWarningEvent

    override suspend fun proceedOrShowPhishingWarning(address: String, onProceed: () -> Unit) {
        val phishingAddress = sendInteractor.isAddressFromPhishingList(address)

        if (phishingAddress) {
            _showPhishingWarningEvent.value = Event(address)
        } else {
            onProceed()
        }
    }
}
