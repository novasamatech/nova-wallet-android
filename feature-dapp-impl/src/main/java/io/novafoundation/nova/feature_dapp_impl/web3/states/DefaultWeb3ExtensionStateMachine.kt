package io.novafoundation.nova.feature_dapp_impl.web3.states

import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.StateMachineTransition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultWeb3ExtensionStateMachine<S>(
    initialState: S
) : Web3ExtensionStateMachine<S>, StateMachineTransition<S> {

    override val state = MutableStateFlow(initialState)

    private val mutex = Mutex()

    override suspend fun transition(transition: suspend StateMachineTransition<S>.(state: S) -> Unit) = mutex.withLock {
        transition(this, state.value)
    }

    override fun emitState(newState: S) {
        state.value = newState
    }
}
