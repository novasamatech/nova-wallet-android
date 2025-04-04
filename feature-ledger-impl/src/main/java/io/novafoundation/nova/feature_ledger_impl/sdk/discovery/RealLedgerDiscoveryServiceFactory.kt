package io.novafoundation.nova.feature_ledger_impl.sdk.discovery

import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryServiceFactory
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.ble.BleLedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.usb.UsbLedgerDeviceDiscoveryService

class RealLedgerDiscoveryServiceFactory(
    private val bleLedgerDeviceDiscoveryService: BleLedgerDeviceDiscoveryService,
    private val usbLedgerDeviceDiscoveryService: UsbLedgerDeviceDiscoveryService
) : LedgerDeviceDiscoveryServiceFactory {

    override fun create(discoveryMethods: DiscoveryMethods): LedgerDeviceDiscoveryService {
        val services = discoveryMethods.methods.map {
            when (it) {
                DiscoveryMethods.Method.BLE -> bleLedgerDeviceDiscoveryService
                DiscoveryMethods.Method.USB -> usbLedgerDeviceDiscoveryService
            }
        }

        return if (services.size == 1) {
            services.single()
        } else {
            CompoundLedgerDiscoveryService(services)
        }
    }
}
