package io.novafoundation.nova.feature_ledger_impl.sdk.discovery

import android.util.Log
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
            Log.d("Ledger", "Subscribed discovery methods: $subscribedMethods")
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

    override fun startDiscovery(methods: Set<DiscoveryMethod>) {
        discoveringSubscribersTracker.withTransaction {
            methods.forEach { method ->
                if (discoveringSubscribersTracker.noSubscribers(method)) {
                    getDelegate(method).startDiscovery()
                }

                discoveringSubscribersTracker.addSubscriber(method)
            }
        }
    }

    override fun stopDiscovery(methods: Set<DiscoveryMethod>) {
        discoveringSubscribersTracker.withTransaction {
            methods.forEach { method ->
                val delegate = getDelegate(method)
                stopDiscovery(delegate)
            }
        }
    }

    override fun stopDiscovery() {
        discoveringSubscribersTracker.withTransaction {
            delegates.forEach(::stopDiscovery)
        }
    }

    private fun stopDiscovery(delegate: LedgerDeviceDiscoveryServiceDelegate) {
        val method = delegate.method

        val subscriberRemoved = discoveringSubscribersTracker.removeSubscriber(method)

        if (subscriberRemoved && discoveringSubscribersTracker.noSubscribers(method)) {
            getDelegate(method).stopDiscovery()
        }
    }

    private fun getDelegate(method: DiscoveryMethod) = delegates.first { it.method == method }
}

private class DiscoveringSubscribersTracker {

    private var subscribersByMethod = mutableMapOf<DiscoveryMethod, Int>().withDefault { 0 }

    private val _subscribedMethods = MutableStateFlow(emptySet<DiscoveryMethod>())
    val subscribedMethods = _subscribedMethods.asStateFlow()

    private var txInProgress: Boolean = false

    /**
     * During the transaction, no values will be emitted to `subscribedMethods`
     */
    fun beginTransaction() {
        require(!txInProgress) { "Nested transactions are not supported" }

        txInProgress = true
    }

    /**
     * Commits the currently present transaction
     */
    fun commitTransaction() {
        require(txInProgress) { "Transaction not strated" }

        txInProgress = false
        emitNewEnabledValue()
    }

    fun addSubscriber(method: DiscoveryMethod) {
        subscribersByMethod[method] = subscribersByMethod.getValue(method) + 1
        emitNewEnabledValue()
    }

    /**
     * Reduces subscriber counter by 1
     * @return true if counter was reduced. False if counter was already zero
     */
    fun removeSubscriber(method: DiscoveryMethod): Boolean {
        val subscribers = subscribersByMethod.getValue(method)
        if (subscribers == 0) return false

        val newSubscribers = subscribers - 1
        if (newSubscribers == 0) {
            subscribersByMethod.remove(method)
        } else {
            subscribersByMethod[method] = newSubscribers
        }

        emitNewEnabledValue()
        return true
    }

    fun noSubscribers(method: DiscoveryMethod): Boolean {
        return subscribersByMethod.getValue(method) == 0
    }

    private fun emitNewEnabledValue() {
        if (!txInProgress) return

        // Using `toSet()` here as StateFlow would not notify subscribers for the same object passed multiple times
        _subscribedMethods.value = subscribersByMethod.keys.toSet()
    }
}

private inline fun DiscoveringSubscribersTracker.withTransaction(block: DiscoveringSubscribersTracker.() -> Unit) {
    beginTransaction()

    try {
        block()
    } finally {
        commitTransaction()
    }
}
