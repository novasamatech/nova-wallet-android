package io.novafoundation.nova.feature_ledger_impl.sdk.discovery

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.usb.DiscoveringSubscribersManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge

class CompoundLedgerDiscoveryService(
    private val delegates: List<LedgerDeviceDiscoveryService>
) : LedgerDeviceDiscoveryService {

    private var discoveringSubscribersManager = DiscoveringSubscribersManager()

    constructor(vararg delegates: LedgerDeviceDiscoveryService) : this(delegates.toList())

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
        if (discoveringSubscribersManager.noSubscribers()) {
            delegates.forEach { it.startDiscovery() }
        }

        discoveringSubscribersManager.addSubscriber()
    }

    override fun stopDiscovery() {
        discoveringSubscribersManager.removeSubscriber()

        if (discoveringSubscribersManager.noSubscribers()) {
            delegates.forEach { it.stopDiscovery() }
        }
    }
}

private class DiscoveringSubscribersManager {

    private var subscribers = 0

    fun addSubscriber() {
        subscribers++
    }

    fun removeSubscriber() {
        if (subscribers == 0) return

        subscribers--
    }

    fun noSubscribers(): Boolean {
        return subscribers == 0
    }
}
