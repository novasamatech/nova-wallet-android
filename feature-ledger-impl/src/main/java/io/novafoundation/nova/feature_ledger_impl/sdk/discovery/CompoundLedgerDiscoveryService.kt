package io.novafoundation.nova.feature_ledger_impl.sdk.discovery

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge

class CompoundLedgerDiscoveryService(
    private val delegates: List<LedgerDeviceDiscoveryServiceDelegate>
) : LedgerDeviceDiscoveryService {

    private var discoveringSubscribersTracker = DiscoveringSubscribersTracker()

    override val discoveredDevices: Flow<List<LedgerDevice>> by lazy {
        combine(
            delegates.map(LedgerDeviceDiscoveryServiceDelegate::discoveredDevices)
        ) { discoveredDevices ->
            discoveredDevices.flatMap { it.toList() }
        }
    }

    override val errors: Flow<Throwable> by lazy {
        delegates.map(LedgerDeviceDiscoveryServiceDelegate::errors)
            .merge()
    }

    override fun startDiscovery(method: DiscoveryMethods.Method) {
        if (discoveringSubscribersTracker.noSubscribers(method)) {
            getDelegate(method).startDiscovery()
        }

        discoveringSubscribersTracker.addSubscriber(method)
    }

    override fun stopDiscovery(method: DiscoveryMethods.Method) {
        val delegate = getDelegate(method)
        stopDiscovery(delegate)
    }

    override fun stopDiscovery() {
        delegates.forEach(::stopDiscovery)
    }

    private fun stopDiscovery(delegate: LedgerDeviceDiscoveryServiceDelegate) {
        val method = delegate.method

        discoveringSubscribersTracker.removeSubscriber(method)

        if (discoveringSubscribersTracker.noSubscribers(method)) {
            getDelegate(method).stopDiscovery()
        }
    }

    private fun getDelegate(method: DiscoveryMethods.Method) = delegates.first { it.method == method }
}

private class DiscoveringSubscribersTracker {

    private var subscribersByMethod = mutableMapOf<DiscoveryMethods.Method, Int>().withDefault { 0 }

    fun addSubscriber(method: DiscoveryMethods.Method) {
        subscribersByMethod[method] = subscribersByMethod.getValue(method) + 1
    }

    fun removeSubscriber(method: DiscoveryMethods.Method) {
        val subscribers = subscribersByMethod.getValue(method)
        if (subscribers == 0) return

        subscribersByMethod[method] = subscribers - 1
    }

    fun noSubscribers(method: DiscoveryMethods.Method): Boolean {
        return subscribersByMethod[method] == 0
    }
}
