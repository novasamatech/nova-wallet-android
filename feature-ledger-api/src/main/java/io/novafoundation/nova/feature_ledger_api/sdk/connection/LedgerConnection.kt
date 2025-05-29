package io.novafoundation.nova.feature_ledger_api.sdk.connection

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface LedgerConnection {

    enum class Type {
        BLE,
        USB
    }

    val channel: Short?

    val type: Type

    val isActive: Flow<Boolean>

    suspend fun mtu(): Int

    suspend fun send(chunks: List<ByteArray>)

    suspend fun connect(): Result<Unit>

    suspend fun resetReceiveChannel()

    val receiveChannel: Channel<ByteArray>
}

suspend fun LedgerConnection.ensureConnected() {
    if (!isConnected()) connect().getOrThrow()
}
suspend fun LedgerConnection.awaitConnected() = isActive.first { connected -> connected }
suspend fun LedgerConnection.isConnected() = isActive.first()
