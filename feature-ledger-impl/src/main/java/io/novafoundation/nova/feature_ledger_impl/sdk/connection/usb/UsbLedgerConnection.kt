package io.novafoundation.nova.feature_ledger_impl.sdk.connection.usb

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Build
import android.util.Log
import io.novafoundation.nova.feature_ledger_api.sdk.connection.LedgerConnection
import io.novafoundation.nova.feature_ledger_api.sdk.connection.awaitConnected
import io.novafoundation.nova.feature_ledger_impl.sdk.connection.BaseLedgerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class UsbLedgerConnection(
    private val appContext: Context,
    private val device: UsbDevice,
    private val coroutineScope: CoroutineScope,
) : BaseLedgerConnection() {

    companion object {

        const val ACTION_USB_PERMISSION = "io.novafoundation.nova.USB_PERMISSION"

        const val LONG_POLLING_INTERVAL = 300L
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

    override suspend fun mtu(): Int {
        return 64
    }

    override suspend fun send(chunks: List<ByteArray>) = sendingMutex.withLock {
        val endpoint = endpointOut
        val connection = usbConnection

        require(endpoint != null && connection != null) {
            "Not connected"
        }

//        val usbRequest = UsbRequest().apply {
//            initialize(connection, endpoint)
//        }
//
//        chunks.forEach { chunk ->
//            usbRequest.queue(ByteBuffer.wrap(chunk), chunk.size)
//            connection.requestWait()
//        }

        for (chunk in chunks) {
            val result = connection.bulkTransfer(endpoint, chunk, chunk.size, 1000)
            if (result < 0) {
                Log.w("Ledger", "Failed to send bytes over usb: $result")
            } else {
                Log.w("Ledger", "Successfully sent ${result} bytes to Ledger")
            }
        }

        var somethingRead: Boolean = false

        while (true) {
            val responseBuffer = ByteArray(mtu())
            val result = usbConnection!!.bulkTransfer(endpointIn!!, responseBuffer, responseBuffer.size, 1000)

            when {
                result > 0 -> {
                    Log.w("Ledger", "Read non empty bytes from usb: ${responseBuffer.joinToString()}")
                    receiveChannel.trySend(responseBuffer.copyOf(result))
                    somethingRead = true
                }
                somethingRead -> {
                    Log.w("Ledger", "Read empty bytes, stopping pollsing")
                    break
                }
                else -> {
                    Log.w("Ledger", "Read empty bytes, waiting for at least one response packet")
                }
            }
        }
    }

    override suspend fun connect(): Result<Unit> = runCatching {
        val permissionIntent = PendingIntent.getBroadcast(appContext, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_MUTABLE)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        val receiver = UsbPermissionReceiver()
        appContext.registerReceiverCompact(receiver, filter)
        usbManager.requestPermission(device, permissionIntent)

        awaitConnected()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun Context.registerReceiverCompact(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    private fun onPermissionGranted() {
        val usbIntf = device.getInterface(0)
        usbInterface = usbIntf
        usbConnection = usbManager.openDevice(device)
        val claimed = usbConnection!!.claimInterface(usbInterface, true)
        if (!claimed) {
            throw Exception("Failed to claim interface")
        }

        Log.w("Ledger", "Endpoints count: ${usbIntf.endpointCount}")
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

        // Start a coroutine to listen for incoming data
        listenForData()
    }

    private fun listenForData() = coroutineScope.launch(Dispatchers.IO) {
//        while (true) {
//            val mtu = mtu()
////            val responseBuffer = ByteBuffer.allocate(mtu)
////            val usbRequest = UsbRequest().apply {
////                initialize(usbConnection!!, endpointIn!!)
////            }
////
////            usbRequest.queue(responseBuffer, mtu)
////            usbConnection!!.requestWait()
////            responseBuffer.rewind()
////            responseBuffer.get()
//
//
//        }
    }

    private inner class UsbPermissionReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_USB_PERMISSION) {
                synchronized(this) {
                    val granted: Boolean = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (granted) {
                        onPermissionGranted()
                    }
                    context?.unregisterReceiver(this)
                }
            }
        }
    }
}
