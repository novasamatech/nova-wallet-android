package io.novafoundation.nova.feature_ledger_api.sdk.connection

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow

interface LedgerConnection {

    enum class Type {
        BLE
    }

    val type: Type

    val isActive: Flow<Boolean>

    suspend fun mtu(): Int

    suspend fun send(chunks: List<ByteArray>)

    suspend fun connect(): Result<Unit>

    val receiveChannel: Channel<ByteArray>
}
