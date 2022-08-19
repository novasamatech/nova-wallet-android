package io.novafoundation.nova.feature_ledger_impl.sdk.discovery.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble.BleConnection
import io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble.LedgerBleManager
import kotlinx.coroutines.flow.MutableStateFlow

class BleLedgerDeviceDiscoveryService(
    private val bluetoothManager: BluetoothManager,
    private val bleManager: LedgerBleManager,
) : LedgerDeviceDiscoveryService {

    override val discoveredDevices = MutableStateFlow(emptyList<LedgerDevice>())

    private var scanCallback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    override fun startDiscovery() {
        val scanFilters = LedgerBleManager.supportedLedgerDevices.map {
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(it.serviceUuid))
                .build()
        }
        val scanSettings = ScanSettings.Builder().build()
        scanCallback = LedgerScanCallback()

        bluetoothManager.adapter.bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    override fun stopDiscovery() {
        bluetoothManager.adapter.bluetoothLeScanner.stopScan(scanCallback)
    }

    private inner class LedgerScanCallback : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val connection = BleConnection(
                bleManager = bleManager,
                bluetoothDevice = result.device
            )
            val device = LedgerDevice(
                id = result.device.address,
                connection = connection
            )

            discoveredDevices.value = discoveredDevices.value + device
        }
    }
}
