package io.novafoundation.nova.common.utils.bluetooth

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.systemCall.EnableBluetoothSystemCall
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.utils.whenStarted
import android.bluetooth.BluetoothManager as NativeBluetoothManager

interface BluetoothManager {

    fun startBleScan(filters: List<ScanFilter>, settings: ScanSettings, callback: ScanCallback)

    fun stopBleScan(callback: ScanCallback)

    fun enableBluetooth()

    fun isBluetoothEnabled(): Boolean

    suspend fun enableBluetoothAndAwait(): Boolean
}

@SuppressLint("MissingPermission")
internal class RealBluetoothManager(
    private val contextManager: ContextManager,
    private val systemCallExecutor: SystemCallExecutor
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

        activity.lifecycle.whenStarted {
            systemCallExecutor.executeSystemCallNotBlocking(EnableBluetoothSystemCall())
        }
    }

    override suspend fun enableBluetoothAndAwait(): Boolean {
        return systemCallExecutor.executeSystemCall(EnableBluetoothSystemCall()).getOrNull() ?: false
    }

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }
}
