package io.novafoundation.nova.feature_ledger_impl.sdk.transport

import io.novafoundation.nova.common.utils.bigEndianBytes
import io.novafoundation.nova.common.utils.buildByteArray
import io.novafoundation.nova.common.utils.dropBytes
import io.novafoundation.nova.common.utils.toBigEndianU16
import io.novafoundation.nova.feature_ledger_api.sdk.connection.LedgerConnection
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.device.isConnected
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransportError
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransportError.Reason
import jp.co.soramitsu.fearless_utils.encrypt.json.copyBytes
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

private const val DATA_TAG_ID: Byte = 0x05

private const val PACKET_INDEX_LENGTH = 2
private const val MESSAGE_SIZE_LENGTH = 2
private const val HEADER_MIN_SIZE = 1 + PACKET_INDEX_LENGTH

private class ReceivedChunk(val content: ByteArray, val total: Int?)

class ChunkedLedgerTransport : LedgerTransport {

    // one request at a time
    private val exchangeMutex = Mutex()

    override suspend fun exchange(data: ByteArray, device: LedgerDevice): ByteArray = exchangeMutex.withLock {
        require(device.isConnected(), Reason.DEVICE_NOT_CONNECTED)

        val mtu = device.connection.mtu()

        val chunks = buildRequestChunks(data, mtu)
        device.connection.send(chunks)

        readChunkedResponse(device.connection)
    }

    private fun require(condition: Boolean, errorReason: Reason) {
        if (!condition) {
            throw LedgerTransportError(errorReason)
        }
    }

    private fun buildRequestChunks(data: ByteArray, mtu: Int): List<ByteArray> {
        val chunks = mutableListOf<ByteArray>()
        val totalLength = data.size
        var offset = 0

        while (offset < totalLength) {
            val currentIndex = chunks.size
            val isFirst = currentIndex == 0

            val chunk = buildByteArray { stream ->
                stream.write(byteArrayOf(DATA_TAG_ID))
                stream.write(currentIndex.toShort().bigEndianBytes)

                if (isFirst) {
                    stream.write(totalLength.toShort().bigEndianBytes)
                }

                val remainingPacketSize = mtu - stream.size()

                if (remainingPacketSize > 0) {
                    val remainingMessageSize = totalLength - offset
                    val packetSize = min(remainingPacketSize, remainingMessageSize)
                    val packetBytes = data.copyBytes(from = offset, size = packetSize)

                    stream.write(packetBytes)
                    offset += packetSize
                }
            }

            chunks += chunk
        }

        return chunks
    }

    private suspend fun readChunkedResponse(connection: LedgerConnection): ByteArray {
        var result = ByteArray(0)

        val headerRaw = connection.receiveChannel.receive()
        val headerChunk = parseReceivedChunk(headerRaw)
        val total = headerChunk.total
        result += headerChunk.content
        require(total != null, Reason.NO_HEADER_FOUND)

        while (result.size < total!!) {
            val raw = connection.receiveChannel.receive()
            val chunk = parseReceivedChunk(raw)

            require(chunk.total == null, Reason.INCOMPLETE_RESPONSE)

            result += chunk.content
        }

        return result
    }

    private fun parseReceivedChunk(raw: ByteArray): ReceivedChunk {
        require(raw.size >= HEADER_MIN_SIZE, Reason.NO_HEADER_FOUND)

        var remainedData = raw

        val tag = raw.first()
        require(tag == DATA_TAG_ID, Reason.UNSUPPORTED_RESPONSE)
        remainedData = remainedData.dropBytes(1)

        val packetIndex = raw.copyBytes(from = 0, size = PACKET_INDEX_LENGTH).toBigEndianU16()
        remainedData = remainedData.dropBytes(PACKET_INDEX_LENGTH)

        return if (packetIndex == 0.toUShort()) {
            require(remainedData.size >= MESSAGE_SIZE_LENGTH, Reason.NO_MESSAGE_SIZE_FOUND)
            val messageSize = remainedData.copyBytes(from = 0, size = MESSAGE_SIZE_LENGTH).toBigEndianU16()
            remainedData = remainedData.dropBytes(MESSAGE_SIZE_LENGTH)

            ReceivedChunk(remainedData, total = messageSize.toInt())
        } else {
            ReceivedChunk(remainedData, total = null)
        }
    }
}
