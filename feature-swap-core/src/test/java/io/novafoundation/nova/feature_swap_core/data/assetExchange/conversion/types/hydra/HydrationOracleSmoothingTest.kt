package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

private const val SIX_SECOND_BLOCKS = 6_000L
private const val TWO_SECOND_BLOCKS = 2_000L

private val SMOOTHING_AT_SIX_SECONDS = BigInteger("3369132345751865974884897103284833777")
private val SMOOTHING_AT_TWO_SECONDS = BigInteger("1130506202395144396888287732331455852")

class HydrationOracleSmoothingTest {

    @Test
    fun `period follows the chain block time`() {
        assertEquals(100, HydrationOnChain.feeOraclePeriodInBlocks(SIX_SECOND_BLOCKS))
        assertEquals(300, HydrationOnChain.feeOraclePeriodInBlocks(TWO_SECOND_BLOCKS))
    }

    @Test
    fun `smoothing matches the runtime table for six second blocks`() {
        assertEquals(SMOOTHING_AT_SIX_SECONDS, HydrationOnChain.feeOracleSmoothing(SIX_SECOND_BLOCKS))
    }

    @Test
    fun `smoothing matches the runtime table for two second blocks`() {
        assertEquals(SMOOTHING_AT_TWO_SECONDS, HydrationOnChain.feeOracleSmoothing(TWO_SECOND_BLOCKS))
    }

    @Test
    fun `smoothing rounds to nearest rather than truncating`() {
        val truncated = 2.toBigInteger() * HydrationOnChain.FRACTION_ONE / 101.toBigInteger()

        assertEquals(truncated + BigInteger.ONE, HydrationOnChain.feeOracleSmoothing(SIX_SECOND_BLOCKS))
    }

    @Test
    fun `smoothing stays below one`() {
        listOf(SIX_SECOND_BLOCKS, TWO_SECOND_BLOCKS, 500L, 12_000L).forEach { blockTime ->
            assertTrue(HydrationOnChain.feeOracleSmoothing(blockTime) < HydrationOnChain.FRACTION_ONE)
        }
    }

    @Test
    fun `degenerate block time still yields a usable period`() {
        listOf(0L, -1L, Long.MAX_VALUE).forEach { blockTime ->
            val period = HydrationOnChain.feeOraclePeriodInBlocks(blockTime)

            assertTrue("period must stay positive for $blockTime", period >= 1)
            assertTrue("smoothing must stay <= 1 for $blockTime", HydrationOnChain.feeOracleSmoothing(blockTime) <= HydrationOnChain.FRACTION_ONE)
        }
    }

    @Test
    fun `saturation follows the smoothing`() {
        assertEquals(4402, saturationBlocks(SMOOTHING_AT_SIX_SECONDS))
        assertEquals(13205, saturationBlocks(SMOOTHING_AT_TWO_SECONDS))
    }

    @Test
    fun `saturation keeps the same wall clock horizon across block times`() {
        val atSixSeconds = saturationBlocks(SMOOTHING_AT_SIX_SECONDS) * SIX_SECOND_BLOCKS
        val atTwoSeconds = saturationBlocks(SMOOTHING_AT_TWO_SECONDS) * TWO_SECOND_BLOCKS

        assertTrue(atSixSeconds - atTwoSeconds < 60_000)
    }

    @Test
    fun `full smoothing saturates immediately`() {
        assertEquals(1, saturationBlocks(HydrationOnChain.FRACTION_ONE))
    }
}
