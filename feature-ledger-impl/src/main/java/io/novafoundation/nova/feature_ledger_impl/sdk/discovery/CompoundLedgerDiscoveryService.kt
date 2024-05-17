package io.novafoundation.nova.feature_ledger_impl.sdk.discovery

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge

class CompoundLedgerDiscoveryService(
    private val delegates: List<LedgerDeviceDiscoveryService>
) : LedgerDeviceDiscoveryService {

    constructor(vararg delegates: LedgerDeviceDiscoveryService): this(delegates.toList())

    override val discoveredDevices: Flow<List<LedgerDevice>> by lazy {
        combine(
            delegates.map(LedgerDeviceDiscoveryService::discoveredDevices)
        ) { discoveredDevices ->
            discoveredDevices.toList().flatten()
        }
    }

    override val errors: Flow<Throwable> by lazy {
        delegates.map(LedgerDeviceDiscoveryService::errors)
            .merge()
    }

    override fun startDiscovery() {
        delegates.forEach { it.startDiscovery() }
    }

    override fun stopDiscovery() {
        delegates.forEach { it.stopDiscovery() }
    }
}
