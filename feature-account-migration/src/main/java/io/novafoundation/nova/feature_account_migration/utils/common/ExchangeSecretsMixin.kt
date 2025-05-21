package io.novafoundation.nova.feature_account_migration.utils.common

import io.novafoundation.nova.feature_account_migration.utils.common.ExchangeSecretsMixin.ExternalEvent
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.ExchangePayload
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.KeyExchangeStateMachine
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.events.KeyExchangeEvent
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.sideEffects.KeyExchangeSideEffect
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.sideEffects.KeyExchangeSideEffect.Receiver
import io.novafoundation.nova.feature_account_migration.utils.stateMachine.sideEffects.KeyExchangeSideEffect.Sender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface ExchangeSecretsMixin<T : ExchangePayload> {

    interface ExternalEvent<in T : ExchangePayload> {

        object RequestExchangeKeys : ExternalEvent<ExchangePayload>

        class SendPublicKey(val publicKey: ByteArray) : ExternalEvent<ExchangePayload>

        class SendEncryptedSecret<T : ExchangePayload>(val exchangePayload: T, val encryptedSecret: ByteArray, val publicKey: ByteArray) : ExternalEvent<T>

        class PeerSecretsReceived<T : ExchangePayload>(val exchangePayload: T, val decryptedSecret: ByteArray) : ExternalEvent<T>
    }

    fun interface SecretProvider {
        suspend fun getSecret(): ByteArray
    }

    fun interface ExchangePayloadProvider<T : ExchangePayload> {
        suspend fun getExchangePayload(): T
    }

    val exchangeEvents: SharedFlow<ExternalEvent<T>>

    fun startSharingSecrets()

    fun acceptKeyExchange()

    fun onPeerAcceptedKeyExchange(publicKey: ByteArray)

    fun onPeerSecretsReceived(secret: ByteArray, publicKey: ByteArray, exchangePayload: T)
}

class RealExchangeSecretsMixin<T : ExchangePayload>(
    private val keyExchangeUtils: KeyExchangeUtils,
    private val secretProvider: ExchangeSecretsMixin.SecretProvider,
    private val exchangePayloadProvider: ExchangeSecretsMixin.ExchangePayloadProvider<T>,
    private val coroutineScope: CoroutineScope
) : ExchangeSecretsMixin<T>, CoroutineScope by coroutineScope {

    private val stateMachine: KeyExchangeStateMachine<T> = KeyExchangeStateMachine(coroutineScope)

    override val exchangeEvents = MutableSharedFlow<ExternalEvent<T>>()

    init {
        stateMachine.sideEffects.onEach { handleSideEffect(it) }
            .launchIn(coroutineScope)
    }

    override fun startSharingSecrets() {
        stateMachine.onEvent(KeyExchangeEvent.Sender.InitKeyExchange)
    }

    override fun acceptKeyExchange() {
        val keyPair = keyExchangeUtils.generateEphemeralKeyPair()
        stateMachine.onEvent(KeyExchangeEvent.Receiver.AcceptKeyExchangeRequest(keyPair))
    }

    override fun onPeerAcceptedKeyExchange(publicKey: ByteArray) {
        val peerPublicKey = keyExchangeUtils.mapPublicKeyFromBytes(publicKey)
        stateMachine.onEvent(KeyExchangeEvent.Sender.PeerAcceptedKeyExchange(peerPublicKey))
    }

    override fun onPeerSecretsReceived(secret: ByteArray, publicKey: ByteArray, exchangePayload: T) {
        val peerPublicKey = keyExchangeUtils.mapPublicKeyFromBytes(publicKey)
        stateMachine.onEvent(KeyExchangeEvent.Receiver.PeerSecretsReceived(secret, peerPublicKey, exchangePayload))
    }

    private fun handleSideEffect(sideEffect: KeyExchangeSideEffect<T>) {
        when (sideEffect) {
            is Sender -> handleSenderSideEffect(sideEffect)

            is Receiver -> handleReceiverSideEffect(sideEffect)
        }
    }

    private fun handleSenderSideEffect(sideEffect: Sender) = launch {
        when (sideEffect) {
            Sender.RequestPeerAcceptKeyExchange -> exchangeEvents.emit(ExternalEvent.RequestExchangeKeys)

            is Sender.SendEncryptedSecrets -> {
                val secret = secretProvider.getSecret()
                val exchangePayload = exchangePayloadProvider.getExchangePayload()
                val keyPair = keyExchangeUtils.generateEphemeralKeyPair()
                val encryptedSecret = keyExchangeUtils.encrypt(secret, keyPair, sideEffect.peerPublicKey)

                exchangeEvents.emit(
                    ExternalEvent.SendEncryptedSecret(
                        exchangePayload,
                        encryptedSecret,
                        keyPair.public.bytes()
                    )
                )
            }
        }
    }

    private fun handleReceiverSideEffect(sideEffect: Receiver<T>) = launch {
        when (sideEffect) {
            is Receiver.AcceptKeyExchange -> exchangeEvents.emit(ExternalEvent.SendPublicKey(sideEffect.publicKey.bytes()))
            is Receiver.PeerSecretsReceived -> {
                val decryptedEntropy = keyExchangeUtils.decrypt(sideEffect.encryptedSecret, sideEffect.keyPair, sideEffect.peerPublicKey)
                exchangeEvents.emit(ExternalEvent.PeerSecretsReceived(sideEffect.exchangePayload, decryptedEntropy))
            }
        }
    }
}
