package io.novafoundation.nova.feature_ledger_api.sdk.discovery

interface LedgerDeviceDiscoveryServiceFactory {

    fun create(discoveryMethod: DiscoveryMethod): LedgerDeviceDiscoveryService
}
