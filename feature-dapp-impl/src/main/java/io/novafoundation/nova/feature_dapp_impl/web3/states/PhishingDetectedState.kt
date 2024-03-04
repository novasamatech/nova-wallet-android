package io.novafoundation.nova.feature_dapp_impl.web3.states

import io.novafoundation.nova.feature_dapp_impl.web3.Web3Transport
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.ExternalEvent
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.State
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.StateMachineTransition

abstract class PhishingDetectedState<R : Web3Transport.Request<*>, S> : State<R, S> {

    abstract fun rejectError(): Throwable

    override suspend fun acceptRequest(request: R, transition: StateMachineTransition<S>) {
        request.reject(rejectError())
    }

    override suspend fun acceptEvent(event: ExternalEvent, transition: StateMachineTransition<S>) {
        // do nothing
    }
}
