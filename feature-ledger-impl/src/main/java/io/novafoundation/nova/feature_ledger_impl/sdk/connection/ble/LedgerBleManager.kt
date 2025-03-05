package io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.toUuid
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.suspend
import java.util.UUID

private const val DEFAULT_MTU = 23
private const val MTU_RESERVED_BYTES = 3

class SupportedBleDevice(
    val serviceUuid: UUID,
    val writeUuid: UUID,
    val notifyUuid: UUID,
)

class LedgerBleManager(
    contextManager: ContextManager
) : BleManager(contextManager.getApplicationContext()), DataReceivedCallback {

    // You can find this and new devices here: https://github.com/LedgerHQ/device-sdk-ts/blob/develop/packages/device-management-kit/src/api/device-model/data/StaticDeviceModelDataSource.ts
    companion object {
        val supportedLedgerDevices by lazy {
            listOf(
                // Nano X
                SupportedBleDevice(
                    serviceUuid = "13d63400-2c97-0004-0000-4c6564676572".toUuid(),
                    notifyUuid = "13d63400-2c97-0004-0001-4c6564676572".toUuid(),
                    writeUuid = "13d63400-2c97-0004-0002-4c6564676572".toUuid()
                ),
                // Stax
                SupportedBleDevice(
                    serviceUuid = "13d63400-2c97-6004-0000-4c6564676572".toUuid(),
                    notifyUuid = "13d63400-2c97-6004-0001-4c6564676572".toUuid(),
                    writeUuid = "13d63400-2c97-6004-0002-4c6564676572".toUuid()
                ),
                // Flex
                SupportedBleDevice(
                    serviceUuid = "13d63400-2c97-3004-0000-4c6564676572".toUuid(),
                    notifyUuid = "13d63400-2c97-3004-0001-4c6564676572".toUuid(),
                    writeUuid = "13d63400-2c97-3004-0002-4c6564676572".toUuid()
                )
            )
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
                val (gattService, supportedDevice) = supportedLedgerDevices.tryFindNonNull { device ->
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
