package io.novafoundation.nova.feature_ledger_impl.sdk.discovery.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDeviceType
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import io.novafoundation.nova.feature_ledger_impl.sdk.connection.usb.UsbLedgerConnection
import io.novafoundation.nova.feature_ledger_impl.sdk.discovery.LedgerDeviceDiscoveryServiceDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.EmptyCoroutineContext

class UsbLedgerDeviceDiscoveryService(
    private val contextManager: ContextManager
) : LedgerDeviceDiscoveryServiceDelegate {

    private val appContext = contextManager.getApplicationContext()

    override val method = DiscoveryMethods.Method.USB

    override val discoveredDevices = MutableStateFlow(emptyList<LedgerDevice>())
    override val errors = MutableSharedFlow<Throwable>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val usbManager: UsbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager

    private var coroutineScope = CoroutineScope(EmptyCoroutineContext)

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    device?.let { discoverDevices() }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    device?.let { discoverDevices() }
                }
            }
        }
    }

    override fun startDiscovery() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        appContext.registerReceiver(usbReceiver, filter)
        discoverDevices()
    }

    override fun stopDiscovery() {
        appContext.unregisterReceiver(usbReceiver)

        coroutineScope.coroutineContext.cancelChildren()
    }

    private fun discoverDevices() {
        try {
            val devices = usbManager.deviceList
                .values.mapNotNull { createLedgerDeviceIfSupported(it) }
            discoveredDevices.tryEmit(devices)
        } catch (e: Exception) {
            errors.tryEmit(e)
        }
    }

    private fun createLedgerDeviceIfSupported(usbDevice: UsbDevice): LedgerDevice? {
        val ledgerDeviceType = usbDevice.getLedgerDeviceType() ?: return null
        val id = "${usbDevice.vendorId}:${usbDevice.productId}"
        val connection = UsbLedgerConnection(appContext, usbDevice, coroutineScope)

        return LedgerDevice(id, ledgerDeviceType, null, connection = connection)
    }

    private fun UsbDevice.getLedgerDeviceType(): LedgerDeviceType? {
        val searchingVendorId = this.vendorId
        val searchingProductId = this.productId

        return LedgerDeviceType.values()
            .firstOrNull { it.usbOptions.vendorId == searchingVendorId && it.usbOptions.productId == searchingProductId }
    }
}
