package io.novafoundation.nova.runtime.network.updaters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            120L to 720_000L,
            118L to 1_000_000L, // reorg / different node, still ahead of the window start
            148L to 1_180_000L,
        )

        val result = SampledBlockTime.initial().feed(observations)

        assertEquals(6000, result.averageBlockTime.toInt())
        assertEquals(1, result.sampleSize.toInt())
    }

    @Test
    fun `duplicate block does not restart the window`() {
        val observations = listOf(
            100L to 600_000L,
            120L to 720_000L,
            120L to 720_000L,
            130L to 780_000L,
        )

        val result = SampledBlockTime.initial().feed(observations)

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
    fun `only the latest windows contribute to the average`() {
        val oldSamples = List(BLOCK_TIME_MAX_SAMPLES_MEMORY.toInt()) { 6000.toBigInteger() }
        val legacyState = SampledBlockTime(samples = oldSamples)
        val windows = BLOCK_TIME_MAX_SAMPLES_MEMORY.toInt()
        val observations = (0L..(windows * BLOCK_TIME_SAMPLING_WINDOW_BLOCKS)).map { block -> block to block * 2000L }

        val result = legacyState.feed(observations)

        assertEquals(List(windows) { 2000.toBigInteger() }, result.samples)
        assertEquals(2000, result.averageBlockTime.toInt())
    }

    @Test
    fun `initial state has no window`() {
        val initial = SampledBlockTime.initial()

        assertEquals(BigInteger.ZERO, initial.sampleSize)
        assertNull(initial.windowStartBlock)
        assertNull(initial.windowStartTimestamp)
        assertNull(initial.lastObservedBlock)
        assertNull(initial.lastObservedTimestamp)
    }

    private fun SampledBlockTime.feed(observations: List<Pair<Long, Long>>): SampledBlockTime {
        return observations.fold(this) { state, (block, timestamp) ->
            state.observing(block = block.toBigInteger(), timestampMillis = timestamp.toBigInteger())
        }
    }
}
