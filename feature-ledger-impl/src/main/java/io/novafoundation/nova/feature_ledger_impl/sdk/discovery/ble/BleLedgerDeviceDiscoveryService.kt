package io.novafoundation.nova.feature_ledger_impl.sdk.discovery.ble

import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import io.novafoundation.nova.common.utils.bluetooth.BluetoothManager
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.LedgerDeviceDiscoveryService
import io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble.BleConnection
import io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble.LedgerBleManager
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class BleScanFailed(val errorCode: Int) : Throwable()

@SuppressLint("MissingPermission")
class BleLedgerDeviceDiscoveryService(
    private val bluetoothManager: BluetoothManager,
    private val ledgerBleManager: LedgerBleManager,
) : LedgerDeviceDiscoveryService {

    override val discoveredDevices = MutableStateFlow(emptyList<LedgerDevice>())
    override val errors = MutableSharedFlow<Throwable>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var scanCallback: ScanCallback? = null

    override fun startDiscovery() {
        val scanFilters = LedgerBleManager.supportedLedgerDevices.map {
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(it.serviceUuid))
                .build()
        }
        val scanSettings = ScanSettings.Builder().build()
        scanCallback = LedgerScanCallback()

        bluetoothManager.startBleScan(scanFilters, scanSettings, scanCallback!!)
    }

    override fun stopDiscovery() {
        scanCallback?.let(bluetoothManager::stopBleScan)
    }

    private inner class LedgerScanCallback : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val alreadyFound = discoveredDevices.value.any { it.id == result.device.address }
            if (alreadyFound) return

            val connection = BleConnection(
                bleManager = ledgerBleManager,
                bluetoothDevice = result.device
            )

            val device = LedgerDevice(
                id = result.device.address,
                name = result.device.name ?: result.device.address,
                connection = connection
            )

            discoveredDevices.value = discoveredDevices.value + device
        }

        override fun onScanFailed(errorCode: Int) {
            errors.tryEmit(BleScanFailed(errorCode))
        }
    }
}
