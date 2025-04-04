package io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.feature_ledger_api.sdk.device.BleDevice
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDeviceType
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.suspend

private const val DEFAULT_MTU = 23
private const val MTU_RESERVED_BYTES = 3

class LedgerBleManager(
    contextManager: ContextManager
) : BleManager(contextManager.getApplicationContext()), DataReceivedCallback {

    companion object {
        fun getSupportedLedgerDevicesInfo(): List<BleDevice.Supported> {
            return LedgerDeviceType.values()
                .mapNotNull { it.bleDevice as? BleDevice.Supported }
        }
    }

    private var characteristicWrite: BluetoothGattCharacteristic? = null
    private var characteristicNotify: BluetoothGattCharacteristic? = null

    var readCallback: DataReceivedCallback? = null

    // 3 bytes are used for internal purposes, so the maximum size is MTU-3.
    val deviceMtu
        get() = mtu - MTU_RESERVED_BYTES

    override fun getGattCallback(): BleManagerGattCallback {
        return object : BleManagerGattCallback() {

            override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                val (gattService, supportedDevice) = getSupportedLedgerDevicesInfo().tryFindNonNull { device ->
                    gatt.getService(device.serviceUuid)?.let { it to device }
                } ?: return false

                characteristicWrite = gattService.getCharacteristic(supportedDevice.writeUuid)
                characteristicNotify = gattService.getCharacteristic(supportedDevice.notifyUuid)

                return characteristicWrite != null && characteristicNotify != null
            }

            override fun onServicesInvalidated() {
                characteristicWrite = null
                characteristicNotify = null
            }

            override fun initialize() {
                beginAtomicRequestQueue()
                    .add(requestMtu(DEFAULT_MTU))
                    .add(enableNotifications(characteristicNotify))
                    .enqueue()
                setNotificationCallback(characteristicNotify)
                    .with(this@LedgerBleManager)
            }
        }
    }

    suspend fun send(chunks: List<ByteArray>) {
        beginAtomicRequestQueue().apply {
            chunks.forEach { chunk ->
                add(writeCharacteristic(characteristicWrite, chunk, WRITE_TYPE_DEFAULT))
            }
        }
            .suspend()
    }

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        readCallback?.onDataReceived(device, data)
    }
}
