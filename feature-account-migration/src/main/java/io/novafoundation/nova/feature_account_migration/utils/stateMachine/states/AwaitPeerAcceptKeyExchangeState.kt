package io.novafoundation.nova.feature_account_migration.utils.stateMachine.states

import io.novafoundation.nova.feature_account_migration.utils.stateMachine.ExchangePayload
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.KeyExchangeTransition
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.events.KeyExchangeEvent
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.sideEffects.KeyExchangeSideEffect

class AwaitPeerAcceptKeyExchangeState<T : ExchangePayload> : KeyExchangeState<T> {

    context(KeyExchangeTransition<T>)
    override suspend fun performTransition(event: KeyExchangeEvent<T>) {
        when (event) {
            is KeyExchangeEvent.Sender.InitKeyExchange -> {
                emitSideEffect(KeyExchangeSideEffect.Sender.RequestPeerAcceptKeyExchange)
            }

            is KeyExchangeEvent.Sender.PeerAcceptedKeyExchange -> {
                emitSideEffect(KeyExchangeSideEffect.Sender.SendEncryptedSecrets(event.peerPublicKey))
                emitState(InitialKeyExchangeState())
            }
        }
    }
}
