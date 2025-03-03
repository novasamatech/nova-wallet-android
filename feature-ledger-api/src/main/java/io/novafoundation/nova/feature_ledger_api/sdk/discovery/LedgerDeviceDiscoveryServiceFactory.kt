package io.novafoundation.nova.feature_ledger_api.sdk.discovery

enum class DiscoveryMethod {
    BLE,
    USB,
    ALL
}

interface LedgerDeviceDiscoveryServiceFactory {

    fun create(discoveryMethod: DiscoveryMethod): LedgerDeviceDiscoveryService
}
