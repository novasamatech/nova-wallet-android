package io.novafoundation.nova.feature_ledger_impl.sdk.discovery

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CompoundLedgerDiscoveryService(
    private val delegates: List<LedgerDeviceDiscoveryService>
): LedgerDeviceDiscoveryService {

    override val discoveredDevices: Flow<List<LedgerDevice>> by lazy {
        combine(
            delegates.map(LedgerDeviceDiscoveryService::discoveredDevices)
        ) { discoveredDevices ->
            discoveredDevices.toList().flatten()
        }
    }

    override fun startDiscovery() {
        delegates.forEach { it.startDiscovery() }
    }

    override fun stopDiscovery() {
        delegates.forEach { it.stopDiscovery() }
    }
}
