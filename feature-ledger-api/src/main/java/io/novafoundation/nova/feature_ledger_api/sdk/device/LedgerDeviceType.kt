package io.novafoundation.nova.feature_ledger_api.sdk.device

import io.novafoundation.nova.common.utils.toUuid
import java.util.UUID

private const val LEDGER_VENDOR_ID = 11415

// You can find this and new devices here: https://github.com/LedgerHQ/device-sdk-ts/blob/develop/packages/device-management-kit/src/api/device-model/data/StaticDeviceModelDataSource.ts
enum class LedgerDeviceType(
    val bleDevice: BleDevice,
    val usbOptions: UsbDeviceInfo
) {
    STAX(
        bleDevice = BleDevice.Supported(
            serviceUuid = "13d63400-2c97-6004-0000-4c6564676572".toUuid(),
            notifyUuid = "13d63400-2c97-6004-0001-4c6564676572".toUuid(),
            writeUuid = "13d63400-2c97-6004-0002-4c6564676572".toUuid()
        ),
        usbOptions = UsbDeviceInfo(vendorId = LEDGER_VENDOR_ID, productId = 24576)
    ),

    FLEX(
        bleDevice = BleDevice.Supported(
            serviceUuid = "13d63400-2c97-3004-0000-4c6564676572".toUuid(),
            notifyUuid = "13d63400-2c97-3004-0001-4c6564676572".toUuid(),
            writeUuid = "13d63400-2c97-3004-0002-4c6564676572".toUuid()
        ),
        usbOptions = UsbDeviceInfo(vendorId = LEDGER_VENDOR_ID, productId = 28672)
    ),

    NANO_X(
        bleDevice = BleDevice.Supported(
            serviceUuid = "13d63400-2c97-0004-0000-4c6564676572".toUuid(),
            notifyUuid = "13d63400-2c97-0004-0001-4c6564676572".toUuid(),
            writeUuid = "13d63400-2c97-0004-0002-4c6564676572".toUuid()
        ),
        usbOptions = UsbDeviceInfo(vendorId = LEDGER_VENDOR_ID, productId = 16401)
    ),

    NANO_S_PLUS(
        bleDevice = BleDevice.NotSupported, // The device doesn't have Bluetooth module to support this connection type
        usbOptions = UsbDeviceInfo(vendorId = LEDGER_VENDOR_ID, productId = 20480)
    )
}

sealed interface BleDevice {

    object NotSupported : BleDevice

    class Supported(
        val serviceUuid: UUID,
        val writeUuid: UUID,
        val notifyUuid: UUID,
    ) : BleDevice
}

class UsbDeviceInfo(
    val vendorId: Int,
    val productId: Int
)
