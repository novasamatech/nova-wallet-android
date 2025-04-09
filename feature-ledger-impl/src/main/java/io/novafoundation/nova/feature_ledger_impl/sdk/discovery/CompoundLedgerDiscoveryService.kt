package io.novafoundation.nova.feature_ledger_impl.sdk.discovery

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods.Method as DiscoveryMethod

class CompoundLedgerDiscoveryService(
    private vararg val delegates: LedgerDeviceDiscoveryServiceDelegate
) : LedgerDeviceDiscoveryService {

    private var discoveringSubscribersTracker = DiscoveringSubscribersTracker()

    override val discoveredDevices: Flow<List<LedgerDevice>> by lazy {
        discoveringSubscribersTracker.subscribedMethods.flatMapLatest { subscribedMethods ->
            val devicesFromSubscribedMethodsFlows = delegates.filter { it.method in subscribedMethods }
                .map { it.discoveredDevices }

            combine(devicesFromSubscribedMethodsFlows) { devicesFromSubscribedMethods ->
                devicesFromSubscribedMethods.flatMap { it }
            }
        }
    }

    override val errors: Flow<Throwable> by lazy {
        delegates.map(LedgerDeviceDiscoveryServiceDelegate::errors).merge()
    }

    override fun startDiscovery(method: DiscoveryMethod) {
        if (discoveringSubscribersTracker.noSubscribers(method)) {
            getDelegate(method).startDiscovery()
        }

        discoveringSubscribersTracker.addSubscriber(method)
    }

    override fun stopDiscovery(method: DiscoveryMethod) {
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

    private fun getDelegate(method: DiscoveryMethod) = delegates.first { it.method == method }
}

private class DiscoveringSubscribersTracker {

    private var subscribersByMethod = mutableMapOf<DiscoveryMethod, Int>().withDefault { 0 }

    private val _subscribedMethods = MutableStateFlow(emptySet<DiscoveryMethod>())
    val subscribedMethods = _subscribedMethods.asStateFlow()

    fun addSubscriber(method: DiscoveryMethod) {
        subscribersByMethod[method] = subscribersByMethod.getValue(method) + 1
        emitNewEnabledValue()
    }

    fun removeSubscriber(method: DiscoveryMethod) {
        val subscribers = subscribersByMethod.getValue(method)
        if (subscribers == 0) return

        val newSubscribers = subscribers - 1
        if (newSubscribers == 0) {
            subscribersByMethod.remove(method)
        } else {
            subscribersByMethod[method] = newSubscribers
        }

        emitNewEnabledValue()
    }

    fun noSubscribers(method: DiscoveryMethod): Boolean {
        return subscribersByMethod.getValue(method) == 0
    }

    private fun emitNewEnabledValue() {
        // Using `toSet()` here as StateFlow would not notify subscribers for the same object passed multiple times
        _subscribedMethods.value = subscribersByMethod.keys.toSet()
    }
}
