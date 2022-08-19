package io.novafoundation.nova.feature_ledger_api.sdk.transport

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
    val bytes = ubyteArrayOf(cla, ins, p1, p2).toByteArray() + data

    return exchange(bytes, device)
}
