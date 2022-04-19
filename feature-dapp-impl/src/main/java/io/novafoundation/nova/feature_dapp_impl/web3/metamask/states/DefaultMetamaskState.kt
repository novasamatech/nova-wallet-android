package io.novafoundation.nova.feature_dapp_impl.web3.metamask.states

import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.MetamaskChain
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskTransportRequest
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.ExternalEvent
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.StateMachineTransition
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3StateMachineHost

class DefaultMetamaskState(
    private val hostApi: Web3StateMachineHost,
    override val chain: MetamaskChain
): MetamaskState {
    override suspend fun acceptRequest(request: MetamaskTransportRequest<*>, transition: StateMachineTransition<MetamaskState>) {
        // TODO
    }

    override suspend fun acceptEvent(event: ExternalEvent, transition: StateMachineTransition<MetamaskState>) {
        when(event) {
            ExternalEvent.PhishingDetected -> transition.emitState(PhishingDetectedMetamaskState(chain))
        }
    }
}

