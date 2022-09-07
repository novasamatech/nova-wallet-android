package io.novafoundation.nova.common.utils.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Intent
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.whenStarted
import android.bluetooth.BluetoothManager as NativeBluetoothManager

interface BluetoothManager {

    fun startBleScan(filters: List<ScanFilter>, settings: ScanSettings, callback: ScanCallback)

    fun stopBleScan(callback: ScanCallback)

    fun enableBluetooth()

    fun isBluetoothEnabled(): Boolean
}

@SuppressLint("MissingPermission")
internal class RealBluetoothManager(
    private val contextManager: ContextManager
) : BluetoothManager {

    private val nativeBluetoothManager = contextManager.getApplicationContext().getSystemService(Activity.BLUETOOTH_SERVICE) as NativeBluetoothManager

    private val bluetoothAdapter: BluetoothAdapter
        get() = nativeBluetoothManager.adapter

    override fun startBleScan(filters: List<ScanFilter>, settings: ScanSettings, callback: ScanCallback) {
        bluetoothAdapter.bluetoothLeScanner?.startScan(filters, settings, callback)
    }

    override fun stopBleScan(callback: ScanCallback) {
        bluetoothAdapter.bluetoothLeScanner?.stopScan(callback)
    }

    override fun enableBluetooth() {
        val activity = contextManager.getActivity()!!
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

        activity.lifecycle.whenStarted {
            activity.startActivityForResult(intent, 0)
        }
    }

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }
}
