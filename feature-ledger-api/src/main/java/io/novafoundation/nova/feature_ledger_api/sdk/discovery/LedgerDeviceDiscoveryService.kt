package io.novafoundation.nova.feature_ledger_api.sdk.discovery

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface LedgerDeviceDiscoveryService {

    val discoveredDevices: Flow<List<LedgerDevice>>

    fun startDiscovery()

    fun stopDiscovery()
}

suspend fun LedgerDeviceDiscoveryService.findDevice(id: String): LedgerDevice? {
    val devices = discoveredDevices.first()

    return devices.find { it.id == id }
}


