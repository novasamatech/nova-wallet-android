package io.novafoundation.nova.feature_account_migration.utils.stateMachine

import io.novafoundation.nova.common.domain.stateMachine.base.StateMachine
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.events.KeyExchangeEvent
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.sideEffects.KeyExchangeSideEffect
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.states.InitialKeyExchangeState
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.states.KeyExchangeState
import kotlinx.coroutines.CoroutineScope

typealias KeyExchangeTransition<T> = StateMachine.Transition<KeyExchangeState<T>, KeyExchangeSideEffect<T>>
typealias KeyExchangeStateMachine<T> = StateMachine<KeyExchangeState<T>, KeyExchangeSideEffect<T>, KeyExchangeEvent<T>>

fun <T : ExchangePayload> KeyExchangeStateMachine(
    coroutineScope: CoroutineScope
): StateMachine<KeyExchangeState<T>, KeyExchangeSideEffect<T>, KeyExchangeEvent<T>> {
    return StateMachine(initialState = InitialKeyExchangeState(), coroutineScope)
}
