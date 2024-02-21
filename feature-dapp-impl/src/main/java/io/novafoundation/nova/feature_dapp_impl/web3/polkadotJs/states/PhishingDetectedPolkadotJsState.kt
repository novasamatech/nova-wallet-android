package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.states

import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsTransportRequest
import io.novafoundation.nova.feature_dapp_impl.web3.states.PhishingDetectedState

class PhishingDetectedPolkadotJsState : PhishingDetectedState<PolkadotJsTransportRequest<*>, PolkadotJsState>(), PolkadotJsState {

    override fun rejectError(): Throwable {
        return IllegalAccessException("Phishing detected!")
    }
}
