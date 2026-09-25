package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AavePoolAbiTest {

    @Test
    fun `should encode reserve data call`() {
        val reserve = "0000000000000000000000000000000100000005"

        val call = AavePoolAbi.getReserveDataCall(reserve)

        assertEquals("0x35ea6a75" + reserve.padStart(64, '0'), call)
    }

    @Test
    fun `should decode reserve list using ABI offset`() {
        val first = "0000000000000000000000000000000100000005"
        val second = "abcdefabcdefabcdefabcdefabcdefabcdefabcd"
        val response = abiWord(32) + abiWord(2) + addressWord(first) + addressWord(second)

        assertEquals(listOf(first, second), AavePoolAbi.decodeReservesList(response))
    }

    @Test
    fun `should decode aToken address from reserve data`() {
        val aToken = "1234567890abcdef1234567890abcdef12345678"
        val response = buildString {
            repeat(8) { append(abiWord(0)) }
            append(addressWord(aToken))
        }

        assertEquals(aToken, AavePoolAbi.decodeATokenAddress(response))
    }

    @Test
    fun `should reject truncated ABI response`() {
        val result = runCatching { AavePoolAbi.decodeReservesList(abiWord(32)) }

        assertTrue(result.isFailure)
    }

    private fun abiWord(value: Int): String = value.toString(16).padStart(64, '0')

    private fun addressWord(address: String): String = address.padStart(64, '0')
}
