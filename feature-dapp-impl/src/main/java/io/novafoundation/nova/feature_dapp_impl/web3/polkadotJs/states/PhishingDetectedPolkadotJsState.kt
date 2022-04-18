package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.states

import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsTransportRequest
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine

class PhishingDetectedPolkadotJsState : PolkadotJsState {

    override suspend fun acceptRequest(request: PolkadotJsTransportRequest<*>, transition: Web3ExtensionStateMachine.StateMachineTransition<PolkadotJsState>) {
        request.reject(IllegalStateException("Phishing detected!"))
    }

    override suspend fun acceptEvent(event: Web3ExtensionStateMachine.ExternalEvent, transition: Web3ExtensionStateMachine.StateMachineTransition<PolkadotJsState>) {
        // do nothing
    }
}
