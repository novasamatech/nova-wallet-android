package io.novafoundation.nova.feature_ledger_impl.sdk.connection

import io.novafoundation.nova.feature_ledger_api.sdk.connection.LedgerConnection
import kotlinx.coroutines.channels.Channel

abstract class BaseLedgerConnection : LedgerConnection {

    @Volatile
    private var _receiveChannel = newChannel()
    private val receiveChannelLock = Any()

    override val receiveChannel
        get() = synchronized(receiveChannelLock) { _receiveChannel }

    override suspend fun resetReceiveChannel() = synchronized(receiveChannelLock) {
        _receiveChannel.close()
        _receiveChannel = newChannel()
    }

    private fun newChannel() = Channel<ByteArray>(Channel.BUFFERED)
}
