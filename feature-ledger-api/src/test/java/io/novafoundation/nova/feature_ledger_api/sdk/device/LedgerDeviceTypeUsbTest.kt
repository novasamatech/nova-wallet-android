package io.novafoundation.nova.feature_ledger_api.sdk.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val LEDGER_VENDOR_ID = 0x2c97

class LedgerDeviceTypeUsbTest {

    @Test
    fun `should match nano x by model byte regardless of interface bitfield`() {
        listOf(0x4011, 0x4015, 0x4001, 0x4000).forEach { productId ->
            assertEquals(LedgerDeviceType.NANO_X, findLedgerDeviceTypeByUsb(LEDGER_VENDOR_ID, productId))
        }
    }

    @Test
    fun `should match every model by model byte`() {
        assertEquals(LedgerDeviceType.NANO_S, findLedgerDeviceTypeByUsb(LEDGER_VENDOR_ID, 0x1015))
        assertEquals(LedgerDeviceType.NANO_S_PLUS, findLedgerDeviceTypeByUsb(LEDGER_VENDOR_ID, 0x5011))
        assertEquals(LedgerDeviceType.STAX, findLedgerDeviceTypeByUsb(LEDGER_VENDOR_ID, 0x6011))
        assertEquals(LedgerDeviceType.FLEX, findLedgerDeviceTypeByUsb(LEDGER_VENDOR_ID, 0x7011))
        assertEquals(LedgerDeviceType.NANO_GEN5, findLedgerDeviceTypeByUsb(LEDGER_VENDOR_ID, 0x8011))
    }

    @Test
    fun `should not match unknown model or foreign vendor`() {
        assertNull(findLedgerDeviceTypeByUsb(LEDGER_VENDOR_ID, 0x2011))
        assertNull(findLedgerDeviceTypeByUsb(0x1234, 0x4011))
    }
}
