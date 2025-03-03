package io.novafoundation.nova.feature_ledger_impl.sdk.discovery

import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethod
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryServiceFactory
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.ble.BleLedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.usb.UsbLedgerDeviceDiscoveryService

class RealLedgerDiscoveryServiceFactory(
    private val bleLedgerDeviceDiscoveryService: BleLedgerDeviceDiscoveryService,
    private val usbLedgerDeviceDiscoveryService: UsbLedgerDeviceDiscoveryService
) : LedgerDeviceDiscoveryServiceFactory {

    override fun create(discoveryMethod: DiscoveryMethod): LedgerDeviceDiscoveryService {
        return when (discoveryMethod) {
            DiscoveryMethod.BLE -> bleLedgerDeviceDiscoveryService
            DiscoveryMethod.USB -> usbLedgerDeviceDiscoveryService
            DiscoveryMethod.ALL -> CompoundLedgerDiscoveryService(listOf(bleLedgerDeviceDiscoveryService, usbLedgerDeviceDiscoveryService))
        }
    }
}
