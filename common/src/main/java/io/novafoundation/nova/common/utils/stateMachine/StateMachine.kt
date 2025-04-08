package io.novafoundation.nova.common.utils.stateMachine

import android.util.Log
import io.novafoundation.nova.common.utils.awaitTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface StateMachine<STATE : StateMachine.State<STATE, SIDE_EFFECT, EVENT>, SIDE_EFFECT, EVENT> {

    val state: StateFlow<STATE>

    val sideEffects: ReceiveChannel<SIDE_EFFECT>

    fun onEvent(event: EVENT)

    interface State<STATE : State<STATE, SIDE_EFFECT, EVENT>, SIDE_EFFECT, EVENT> {

        context(Transition<STATE, SIDE_EFFECT>)
        suspend fun performTransition(event: EVENT)

        /**
         * Called when this state is state-machine's initial state
         */
        context(Transition<STATE, SIDE_EFFECT>)
        suspend fun bootstrap() {}
    }

    interface Transition<STATE : State<*, *, *>, SIDE_EFFECT> {

        suspend fun emitState(newState: STATE)

        suspend fun emitSideEffect(sideEffect: SIDE_EFFECT)
    }
}

fun <STATE : StateMachine.State<STATE, SIDE_EFFECT, EVENT>, SIDE_EFFECT, EVENT> StateMachine(
    initialState: STATE,
    coroutineScope: CoroutineScope
): StateMachine<STATE, SIDE_EFFECT, EVENT> = StateMachineImpl(initialState, coroutineScope)

private class StateMachineImpl<STATE : StateMachine.State<STATE, SIDE_EFFECT, EVENT>, SIDE_EFFECT, EVENT>(
    private val initialState: STATE,
    coroutineScope: CoroutineScope
) : StateMachine<STATE, SIDE_EFFECT, EVENT>, CoroutineScope by coroutineScope {

    private val mutex = Mutex()

    override val state = MutableStateFlow(initialState)

    override val sideEffects = Channel<SIDE_EFFECT>(capacity = Channel.UNLIMITED)

    private val bootstrapped = MutableStateFlow(false)

    init {
        bootstrap()
    }

    override fun onEvent(event: EVENT) {
        Log.d("StateMachineTAG", "onEvent: $event")
        launch {
            bootstrapped.awaitTrue()

            mutex.withLock {
                with(TransitionImpl()) {
                    state.value.performTransition(event)
                }
            }
        }
    }

    private fun bootstrap() {
        launch {
            Log.d("StateMachineTAG", "bootstrap started")
            mutex.withLock {
                with(TransitionImpl()) {
                    state.value.bootstrap()
                }
            }
            bootstrapped.value = true
            Log.d("StateMachineTAG", "bootstrap fini")
        }
    }

    private inner class TransitionImpl : StateMachine.Transition<STATE, SIDE_EFFECT> {

        override suspend fun emitState(newState: STATE) {
            Log.d("StateMachineTAG", "state: $newState")
            state.value = newState
        }

        override suspend fun emitSideEffect(sideEffect: SIDE_EFFECT) {
            Log.d("StateMachineTAG", "sideEffect: $sideEffect")
            sideEffects.send(sideEffect)
        }
    }
}
