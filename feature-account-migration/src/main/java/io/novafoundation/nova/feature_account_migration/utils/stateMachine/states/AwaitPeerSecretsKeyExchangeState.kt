package io.novafoundation.nova.feature_account_migration.utils.stateMachine.states

import io.novafoundation.nova.feature_account_migration.utils.stateMachine.ExchangePayload
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.KeyExchangeTransition
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.events.KeyExchangeEvent
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.sideEffects.KeyExchangeSideEffect
import java.security.KeyPair

class AwaitPeerSecretsKeyExchangeState<T : ExchangePayload>(
    private val keyPair: KeyPair
) : KeyExchangeState<T> {

    context(KeyExchangeTransition<T>)
    override suspend fun performTransition(event: KeyExchangeEvent<T>) {
        when (event) {
            is KeyExchangeEvent.Receiver.PeerSecretsReceived -> {
                emitSideEffect(KeyExchangeSideEffect.Receiver.PeerSecretsReceived(event.exchangePayload, event.encryptedSecrets, keyPair, event.peerPublicKey))
                emitState(InitialKeyExchangeState())
            }
        }
    }
}
