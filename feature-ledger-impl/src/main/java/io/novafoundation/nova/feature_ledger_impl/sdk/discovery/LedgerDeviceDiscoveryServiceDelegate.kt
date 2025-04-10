package io.novafoundation.nova.feature_ledger_impl.sdk.discovery

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LedgerDeviceDiscoveryServiceDelegate {

    val method: DiscoveryMethods.Method

    val discoveredDevices: StateFlow<List<LedgerDevice>>

    val errors: Flow<Throwable>

    fun startDiscovery()

    fun stopDiscovery()
}
