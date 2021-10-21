package io.novafoundation.nova.feature_wallet_impl.presentation.send.phishing.warning.api

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event

interface PhishingWarningMixin {

    val showPhishingWarning: LiveData<Event<String>>

    suspend fun proceedOrShowPhishingWarning(address: String, onProceed: () -> Unit)
}

interface PhishingWarningPresentation : PhishingWarningMixin {

    fun proceedAddress(address: String)

    fun declinePhishingAddress()
}

suspend fun PhishingWarningPresentation.proceedOrShowPhishingWarning(address: String) {
    proceedOrShowPhishingWarning(address) { proceedAddress(address) }
}
