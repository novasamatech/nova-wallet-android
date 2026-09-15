package io.novafoundation.nova.runtime.network.updaters

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import java.math.BigInteger

/**
 * Minimum number of blocks a sampling window must span before it produces a block time sample.
 *
 * Block time is measured as `timestamp span / block span` over a window instead of the delta between two consecutive blocks.
 * Chains with elastic scaling (Hydration, Asset Hub) produce several blocks per relay slot that share the same timestamp,
 * so consecutive deltas look like `0, 0, 6000` and cannot be averaged reliably. Spanning many slots also tolerates skipped
 * block notifications.
 */
const val BLOCK_TIME_SAMPLING_WINDOW_BLOCKS = 30L

/**
 * How many completed windows the running average remembers. Bounding the memory lets the estimate follow a runtime
 * upgrade that changes the block time instead of being pinned to samples collected months ago.
 */
const val BLOCK_TIME_MAX_SAMPLES_MEMORY = 10L

data class SampledBlockTime(
    val sampleSize: BigInteger,
    val averageBlockTime: BigInteger,
    val windowStartBlock: BlockNumber? = null,
    val windowStartTimestamp: BigInteger? = null,
) {

    companion object {

        fun initial() = SampledBlockTime(sampleSize = BigInteger.ZERO, averageBlockTime = BigInteger.ZERO)
    }
}

/**
 * Folds a new `(block, timestamp)` observation into the sampling state.
 *
 * A window is opened at the first observation and closed once it spans at least [BLOCK_TIME_SAMPLING_WINDOW_BLOCKS] blocks,
 * producing one sample. Non-monotonic observations (reorg, node switch, timestamps not advancing) restart the window.
 */
fun SampledBlockTime.observing(block: BlockNumber, timestampMillis: BigInteger): SampledBlockTime {
    val startBlock = windowStartBlock
    val startTimestamp = windowStartTimestamp

    if (startBlock == null || startTimestamp == null || block <= startBlock || timestampMillis < startTimestamp) {
        return restartingWindow(block, timestampMillis)
    }

    val blockSpan = block - startBlock
    if (blockSpan < BLOCK_TIME_SAMPLING_WINDOW_BLOCKS.toBigInteger()) return this

    val timestampSpan = timestampMillis - startTimestamp
    if (timestampSpan <= BigInteger.ZERO) return restartingWindow(block, timestampMillis)

    val sample = timestampSpan / blockSpan
    val maxMemory = BLOCK_TIME_MAX_SAMPLES_MEMORY.toBigInteger()
    val rememberedSamples = sampleSize.min(maxMemory)
    val newAverage = (averageBlockTime * rememberedSamples + sample) / (rememberedSamples + BigInteger.ONE)

    return SampledBlockTime(
        sampleSize = (sampleSize + BigInteger.ONE).min(maxMemory),
        averageBlockTime = newAverage,
        windowStartBlock = block,
        windowStartTimestamp = timestampMillis,
    )
}

private fun SampledBlockTime.restartingWindow(block: BlockNumber, timestampMillis: BigInteger): SampledBlockTime {
    return copy(windowStartBlock = block, windowStartTimestamp = timestampMillis)
}
