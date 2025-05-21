package io.novafoundation.nova.feature_account_migration.utils.stateMachine.states

import io.novafoundation.nova.common.domain.stateMachine.base.StateMachine.State
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.ExchangePayload
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.events.KeyExchangeEvent
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.sideEffects.KeyExchangeSideEffect

interface KeyExchangeState<T : ExchangePayload> : State<KeyExchangeState<T>, KeyExchangeSideEffect<T>, KeyExchangeEvent<T>>
