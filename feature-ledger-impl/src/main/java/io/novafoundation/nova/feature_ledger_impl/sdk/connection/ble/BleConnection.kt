package io.novafoundation.nova.feature_ledger_impl.sdk.connection.ble

import android.bluetooth.BluetoothDevice
import android.util.Log
import io.novafoundation.nova.feature_ledger_api.sdk.connection.LedgerConnection
import io.novafoundation.nova.feature_ledger_api.sdk.connection.awaitConnected
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.ktx.state.ConnectionState
import no.nordicsemi.android.ble.ktx.stateAsFlow
import no.nordicsemi.android.ble.ktx.suspend

class BleConnection(
    private val bleManager: LedgerBleManager,
    private val bluetoothDevice: BluetoothDevice,
) : LedgerConnection, DataReceivedCallback {

    @Volatile
    private var _receiveChannel = newChannel()
    private val receiveChannelLock = Any()

    override val channel: Short? = null

    override suspend fun connect(): Result<Unit> = runCatching {
        bleManager.connect(bluetoothDevice).suspend()

        bleManager.readCallback = this

        awaitConnected()
    }

    override val type: LedgerConnection.Type = LedgerConnection.Type.BLE

    override val isActive: Flow<Boolean>
        get() = bleManager.stateAsFlow()
            .map { it == ConnectionState.Ready }

    override suspend fun mtu(): Int {
        ensureCorrectDevice()

        return bleManager.deviceMtu
    }

    override suspend fun send(chunks: List<ByteArray>) {
        ensureCorrectDevice()

        bleManager.send(chunks)
    }

    override suspend fun resetReceiveChannel() = synchronized(receiveChannelLock) {
        _receiveChannel.close()
        _receiveChannel = newChannel()
    }

    override val receiveChannel
        get() = synchronized(receiveChannelLock) { _receiveChannel }

    override fun onDataReceived(device: BluetoothDevice, data: Data) {
        ensureCorrectDevice()

        data.value?.let {
            Log.w("Ledger", "Read non empty bytes from usb: ${it.joinToString()}")
        }

        data.value?.let(receiveChannel::trySend)
    }

    private fun ensureCorrectDevice() = require(bleManager.bluetoothDevice?.address == bluetoothDevice.address) {
        "Wrong device connected"
    }

    private fun newChannel() = Channel<ByteArray>(Channel.BUFFERED)
}
