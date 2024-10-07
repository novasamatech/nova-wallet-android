package io.novafoundation.nova.feature_ledger_impl.sdk.transport

import io.novafoundation.nova.common.utils.bigEndianBytes
import io.novafoundation.nova.common.utils.buildByteArray
import io.novafoundation.nova.common.utils.dropBytes
import io.novafoundation.nova.common.utils.toBigEndianU16
import io.novafoundation.nova.feature_ledger_api.sdk.connection.LedgerConnection
import io.novafoundation.nova.feature_ledger_api.sdk.connection.ensureConnected
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransport
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransportError
import io.novafoundation.nova.feature_ledger_api.sdk.transport.LedgerTransportError.Reason
import io.novasama.substrate_sdk_android.encrypt.json.copyBytes
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

private const val DATA_TAG_ID: Byte = 0x05

private const val CHANNEL_LENGTH = 2
private const val PACKET_INDEX_LENGTH = 2
private const val MESSAGE_SIZE_LENGTH = 2
private const val HEADER_MIN_SIZE_NO_CHANNEL = 1 + PACKET_INDEX_LENGTH
private const val HEADER_MIN_SIZE_CHANNEL = CHANNEL_LENGTH + HEADER_MIN_SIZE_NO_CHANNEL

private class ReceivedChunk(val content: ByteArray, val total: Int?)

class ChunkedLedgerTransport : LedgerTransport {

    // one request at a time
    private val exchangeMutex = Mutex()

    override suspend fun exchange(data: ByteArray, device: LedgerDevice): ByteArray = exchangeMutex.withLock {
        device.connection.ensureConnected()
        device.connection.resetReceiveChannel()

        val mtu = device.connection.mtu()
        val channel = device.connection.channel

        val chunks = buildRequestChunks(data, mtu, channel)
        device.connection.send(chunks)

        readChunkedResponse(device.connection)
    }

    private fun require(condition: Boolean, errorReason: Reason) {
        if (!condition) {
            throw LedgerTransportError(errorReason)
        }
    }

    private fun buildRequestChunks(
        data: ByteArray,
        mtu: Int,
        channel: Short?
    ): List<ByteArray> {
        val chunks = mutableListOf<ByteArray>()
        val totalLength = data.size
        var offset = 0

        while (offset < totalLength) {
            val currentIndex = chunks.size
            val isFirst = currentIndex == 0

            val chunk = buildByteArray { stream ->
                channel?.let {
                    stream.write(channel.toShort().bigEndianBytes)
                }

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

        val hasChannel = connection.channel != null

        val headerRaw = connection.receiveChannel.receive()
        val headerChunk = parseReceivedChunk(headerRaw, hasChannel, readMax = null)
        val total = headerChunk.total
        result += headerChunk.content
        require(total != null, Reason.NO_HEADER_FOUND)

        while (result.size < total!!) {
            val raw = connection.receiveChannel.receive()
            val readMax = total - result.size
            val chunk = parseReceivedChunk(raw, hasChannel, readMax)

            require(chunk.total == null, Reason.INCOMPLETE_RESPONSE)

            result += chunk.content
        }

        return result
    }

    private fun parseReceivedChunk(
        raw: ByteArray,
        hasChannel: Boolean,
        readMax: Int?
    ): ReceivedChunk {
        require(raw.size >= headerSize(hasChannel), Reason.NO_HEADER_FOUND)

        var remainedData = raw

        if (hasChannel) {
            remainedData = remainedData.dropBytes(CHANNEL_LENGTH)
        }

        val tag = remainedData.first()
        require(tag == DATA_TAG_ID, Reason.UNSUPPORTED_RESPONSE)
        remainedData = remainedData.dropBytes(1)

        val packetIndex = remainedData.copyBytes(from = 0, size = PACKET_INDEX_LENGTH).toBigEndianU16()
        remainedData = remainedData.dropBytes(PACKET_INDEX_LENGTH)

        return if (packetIndex == 0.toUShort()) {
            require(remainedData.size >= MESSAGE_SIZE_LENGTH, Reason.NO_MESSAGE_SIZE_FOUND)
            val messageSize = remainedData.copyBytes(from = 0, size = MESSAGE_SIZE_LENGTH).toBigEndianU16().toInt()
            remainedData = remainedData.dropBytes(MESSAGE_SIZE_LENGTH)

            val content = remainedData.take(messageSize).toByteArray()

            ReceivedChunk(content, total = messageSize)
        } else {
            val content = remainedData.take(readMax!!).toByteArray()

            ReceivedChunk(content, total = null)
        }
    }

    private fun headerSize(hasChannel: Boolean) = if (hasChannel) HEADER_MIN_SIZE_CHANNEL else HEADER_MIN_SIZE_NO_CHANNEL
}
