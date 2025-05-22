package io.novafoundation.nova.feature_account_migration.utils.stateMachine.sideEffects

import io.novafoundation.nova.feature_account_migration.utils.stateMachine.ExchangePayload
import java.security.KeyPair
import java.security.PublicKey

sealed interface KeyExchangeSideEffect<in T : ExchangePayload> {

    sealed interface Sender : KeyExchangeSideEffect<ExchangePayload> {

        data object RequestPeerAcceptKeyExchange : Sender

        class SendEncryptedSecrets(val peerPublicKey: PublicKey) : Sender
    }

    sealed interface Receiver<T : ExchangePayload> : KeyExchangeSideEffect<T> {

        class AcceptKeyExchange(val publicKey: PublicKey) : Receiver<ExchangePayload>

        class PeerSecretsReceived<T : ExchangePayload>(
            val exchangePayload: T,
            val encryptedSecret: ByteArray,
            val keyPair: KeyPair,
            val peerPublicKey: PublicKey
        ) : Receiver<T>
    }
}
