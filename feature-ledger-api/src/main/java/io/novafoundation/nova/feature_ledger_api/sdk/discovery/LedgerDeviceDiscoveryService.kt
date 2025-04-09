package io.novafoundation.nova.feature_ledger_api.sdk.discovery

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface LedgerDeviceDiscoveryService {

    val discoveredDevices: Flow<List<LedgerDevice>>

    val errors: Flow<Throwable>

    fun startDiscovery(methods: Set<DiscoveryMethods.Method>)

    fun stopDiscovery(methods: Set<DiscoveryMethods.Method>)

    fun stopDiscovery()
}

suspend fun LedgerDeviceDiscoveryService.findDevice(id: String): LedgerDevice? {
    val devices = discoveredDevices.first()

    return devices.find { it.id == id }
}

suspend fun LedgerDeviceDiscoveryService.findDeviceOrThrow(id: String): LedgerDevice {
    return findDevice(id) ?: throw IllegalArgumentException("Device not found")
}
