package io.novafoundation.nova.feature_account_migration.utils.stateMachine.events

import io.novafoundation.nova.feature_account_migration.utils.stateMachine.ExchangePayload
import java.security.KeyPair
import java.security.PublicKey

interface KeyExchangeEvent<in T : ExchangePayload> {

    interface Sender : KeyExchangeEvent<ExchangePayload> {
        data object InitKeyExchange : Sender

        data class PeerAcceptedKeyExchange(val peerPublicKey: PublicKey) : Sender
    }

    interface Receiver<T : ExchangePayload> : KeyExchangeEvent<T> {
        data class AcceptKeyExchangeRequest(val keyPair: KeyPair) : Receiver<ExchangePayload>

        class PeerSecretsReceived<T : ExchangePayload>(
            val encryptedSecrets: ByteArray,
            val peerPublicKey: PublicKey,
            val exchangePayload: T
        ) : Receiver<T>
    }
}
