package io.novafoundation.nova.feature_ledger_impl.sdk.connection.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import io.novafoundation.nova.feature_ledger_api.sdk.connection.LedgerConnection
import io.novafoundation.nova.feature_ledger_api.sdk.connection.awaitConnected
import io.novafoundation.nova.feature_ledger_impl.sdk.connection.BaseLedgerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UsbLedgerConnection(
    private val appContext: Context,
    private val device: UsbDevice,
    coroutineScope: CoroutineScope
) : BaseLedgerConnection(), CoroutineScope by coroutineScope {

    companion object {

        const val ACTION_USB_PERMISSION = "io.novafoundation.nova.USB_PERMISSION"

        val PERMISSIONS_GRANTED_SIGNAL = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    }

    override val type: LedgerConnection.Type = LedgerConnection.Type.USB

    override val isActive = MutableStateFlow(false)
    private val usbManager: UsbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager

    private var usbConnection: UsbDeviceConnection? = null

    private var usbInterface: UsbInterface? = null

    private var endpointOut: UsbEndpoint? = null
    private var endpointIn: UsbEndpoint? = null
    private val sendingMutex = Mutex()

    override val channel: Short = 1

    init {
        PERMISSIONS_GRANTED_SIGNAL
            .onEach { onPermissionGranted() }
            .launchIn(coroutineScope)
    }

    override suspend fun mtu(): Int {
        return 64
    }

    override suspend fun send(chunks: List<ByteArray>) = sendingMutex.withLock {
        val endpoint = endpointOut
        val connection = usbConnection

        require(endpoint != null && connection != null) {
            "Not connected"
        }

        for (chunk in chunks) {
            Log.d("Ledger", "Attempting to send chunk of size ${chunk.size} over usb")
            val result = connection.bulkTransfer(endpoint, chunk, chunk.size, 10000)
            if (result < 0) {
                Log.e("Ledger", "Failed to send bytes over usb: $result")
            } else {
                Log.d("Ledger", "Successfully sent $result bytes to Ledger")
            }
        }

        var somethingRead: Boolean = false

        while (true) {
            val responseBuffer = ByteArray(mtu())
            val result = usbConnection!!.bulkTransfer(endpointIn!!, responseBuffer, responseBuffer.size, 50)

            when {
                result > 0 -> {
                    Log.d("Ledger", "Read non empty bytes from usb: ${responseBuffer.joinToString()}")
                    receiveChannel.trySend(responseBuffer.copyOf(result))
                    somethingRead = true
                }
                somethingRead -> {
                    Log.d("Ledger", "Read empty bytes, stopping polling")
                    break
                }
                else -> {
                    delay(50)
                    Log.d("Ledger", "Read empty bytes, waiting for at least one response packet")
                }
            }
        }
    }

    override suspend fun connect(): Result<Unit> = runCatching {
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setClass(appContext, UsbPermissionReceiver::class.java)
        }

        val permissionIntent = PendingIntent.getBroadcast(appContext, 0, intent, PendingIntent.FLAG_MUTABLE)
        usbManager.requestPermission(device, permissionIntent)

        awaitConnected()
    }

    private fun onPermissionGranted() {
        val usbIntf = device.getInterface(0)
        usbInterface = usbIntf
        usbConnection = usbManager.openDevice(device)
        val claimed = usbConnection!!.claimInterface(usbInterface, true)
        if (!claimed) {
            throw Exception("Failed to claim interface")
        }

        Log.d("Ledger", "Endpoints count: ${usbIntf.endpointCount}")
        for (i in 0 until usbIntf.endpointCount) {
            val tmpEndpoint: UsbEndpoint = usbIntf.getEndpoint(i)
            if (tmpEndpoint.direction == UsbConstants.USB_DIR_IN) {
                endpointIn = tmpEndpoint
            } else {
                endpointOut = tmpEndpoint
            }
        }

        if (endpointIn == null) {
            throw Exception("Failed to find in endpoint")
        }

        if (endpointOut == null) {
            throw Exception("Failed to find out endpoint")
        }

        isActive.value = true
    }

    class UsbPermissionReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_USB_PERMISSION) {
                synchronized(this) {
                    val granted: Boolean = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)

                    if (granted) {
                        PERMISSIONS_GRANTED_SIGNAL.tryEmit(Unit)
                    }
                }
            }
        }
    }
}
