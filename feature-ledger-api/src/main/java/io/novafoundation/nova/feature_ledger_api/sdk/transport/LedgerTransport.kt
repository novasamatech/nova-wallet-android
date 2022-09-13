package io.novafoundation.nova.feature_ledger_api.sdk.transport

import io.novafoundation.nova.common.utils.bigEndianBytes
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice

interface LedgerTransport {

    suspend fun exchange(data: ByteArray, device: LedgerDevice): ByteArray
}

@OptIn(ExperimentalUnsignedTypes::class)
suspend fun LedgerTransport.send(
    cla: UByte,
    ins: UByte,
    p1: UByte,
    p2: UByte,
    data: ByteArray,
    device: LedgerDevice
): ByteArray {
    var message = ubyteArrayOf(cla, ins, p1, p2)

    if (data.isNotEmpty()) {
        if (data.size < 256) {
            message += data.size.toUByte()
        } else {
            message += 0x00u
            message += data.size.toShort().bigEndianBytes.toUByteArray()
        }

        message += data.toUByteArray()
    }

    return exchange(message.toByteArray(), device)
}
