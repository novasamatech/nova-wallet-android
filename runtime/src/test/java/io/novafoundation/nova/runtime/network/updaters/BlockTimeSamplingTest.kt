package io.novafoundation.nova.runtime.network.updaters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class BlockTimeSamplingTest {

    @Test
    fun `elastic scaling timestamps average to slot duration divided by blocks per slot`() {
        // Hydration: 3 blocks per 6s relay slot, Timestamp.Now deltas are 0, 0, 6000
        val observations = (0L..60L).map { block -> block to (block / 3) * 6000L }

        val result = SampledBlockTime.initial().feed(observations)

        assertEquals(2000, result.averageBlockTime.toInt())
        assertEquals(2, result.sampleSize.toInt())
    }

    @Test
    fun `regular chain samples its block time exactly`() {
        val observations = (0L..30L).map { block -> block to block * 6000L }

        val result = SampledBlockTime.initial().feed(observations)

        assertEquals(6000, result.averageBlockTime.toInt())
        assertEquals(1, result.sampleSize.toInt())
    }

    @Test
    fun `no sample is produced until the window spans enough blocks`() {
        val observations = (0L until BLOCK_TIME_SAMPLING_WINDOW_BLOCKS).map { block -> block to block * 6000L }

        val result = SampledBlockTime.initial().feed(observations)

        assertEquals(0, result.sampleSize.toInt())
        assertEquals(0, result.windowStartBlock!!.toInt())
    }

    @Test
    fun `skipped block notifications do not prevent sampling`() {
        val observations = listOf(0L, 7L, 19L, 33L).map { block -> block to block * 6000L }

        val result = SampledBlockTime.initial().feed(observations)

        assertEquals(6000, result.averageBlockTime.toInt())
        assertEquals(1, result.sampleSize.toInt())
    }

    @Test
    fun `window restarts when block number goes backwards`() {
        val observations = listOf(
            100L to 600_000L,
            90L to 540_000L, // reorg / different node
            120L to 720_000L,
        )

        val result = SampledBlockTime.initial().feed(observations)

        // Without a restart the window would be 100..120 (20 blocks) and produce nothing
        assertEquals(6000, result.averageBlockTime.toInt())
        assertEquals(1, result.sampleSize.toInt())
    }

    @Test
    fun `window restarts without sampling when timestamps do not advance`() {
        val observations = (0L..30L).map { block -> block to 1_000_000L }

        val result = SampledBlockTime.initial().feed(observations)

        assertEquals(0, result.sampleSize.toInt())
        assertEquals(30, result.windowStartBlock!!.toInt())
    }

    @Test
    fun `old samples do not dominate after the chain changes its block time`() {
        val legacyState = SampledBlockTime(sampleSize = 1000.toBigInteger(), averageBlockTime = 6000.toBigInteger())
        val windows = 30
        val observations = (0L..(windows * BLOCK_TIME_SAMPLING_WINDOW_BLOCKS)).map { block -> block to block * 2000L }

        val result = legacyState.feed(observations)

        assertTrue("expected average close to 2000 but was ${result.averageBlockTime}", result.averageBlockTime < 2300.toBigInteger())
        assertTrue(result.sampleSize <= BLOCK_TIME_MAX_SAMPLES_MEMORY.toBigInteger())
    }

    @Test
    fun `initial state has no window`() {
        val initial = SampledBlockTime.initial()

        assertEquals(BigInteger.ZERO, initial.sampleSize)
        assertNull(initial.windowStartBlock)
        assertNull(initial.windowStartTimestamp)
    }

    private fun SampledBlockTime.feed(observations: List<Pair<Long, Long>>): SampledBlockTime {
        return observations.fold(this) { state, (block, timestamp) ->
            state.observing(block = block.toBigInteger(), timestampMillis = timestamp.toBigInteger())
        }
    }
}
