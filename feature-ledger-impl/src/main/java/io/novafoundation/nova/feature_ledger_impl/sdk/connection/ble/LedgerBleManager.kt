package io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.toUuid
import jp.co.soramitsu.fearless_utils.extensions.tryFindNonNull
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.suspend
import java.util.UUID

private const val DEFAULT_MTU = 23

class SupportedBleDevice(
    val serviceUuid: UUID,
    val writeUuid: UUID,
    val notifyUuid: UUID,
)

class LedgerBleManager(
    contextManager: ContextManager
) : BleManager(contextManager.getApplicationContext()), DataReceivedCallback {

    companion object {
        val supportedLedgerDevices by lazy {
            listOf(
                SupportedBleDevice(
                    serviceUuid = "13D63400-2C97-0004-0000-4C6564676572".toUuid(),
                    writeUuid = "13D63400-2C97-0004-0002-4C6564676572".toUuid(),
                    notifyUuid = "13D63400-2C97-0004-0001-4C6564676572".toUuid()
                )
            )
        }
    }

    private var characteristicWrite: BluetoothGattCharacteristic? = null
    private var characteristicNotify: BluetoothGattCharacteristic? = null

    var readCallback: DataReceivedCallback? = null

    val deviceMtu
        get() = mtu

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
