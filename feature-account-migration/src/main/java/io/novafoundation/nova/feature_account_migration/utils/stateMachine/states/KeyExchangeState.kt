package io.novafoundation.nova.feature_account_migration.utils.stateMachine.states

import io.novafoundation.nova.common.utils.stateMachine.StateMachine
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.ExchangePayload
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.events.KeyExchangeEvent
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.sideEffects.KeyExchangeSideEffect

interface KeyExchangeState<T : ExchangePayload> : StateMachine.State<KeyExchangeState<T>, KeyExchangeSideEffect<T>, KeyExchangeEvent<T>>
