package io.novafoundation.nova.runtime.util

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.formatting.toTimerValue
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface BlockDurationEstimator {

    fun durationUntil(block: BlockNumber): Duration

    fun timestampOf(block: BlockNumber): Long
}

fun BlockDurationEstimator.timerUntil(block: BlockNumber): TimerValue {
    return durationUntil(block).toTimerValue()
}

fun BlockDurationEstimator(currentBlock: BlockNumber, blockTimeMillis: BigInteger): BlockDurationEstimator {
    return RealBlockDurationEstimator(currentBlock, blockTimeMillis)
}

internal class RealBlockDurationEstimator(
    private val currentBlock: BlockNumber,
    private val blockTimeMillis: BigInteger
) : BlockDurationEstimator {

    override fun durationUntil(block: BlockNumber): Duration {
        val blocksInFuture = block - currentBlock

        if (blocksInFuture <= BigInteger.ZERO) return Duration.ZERO

        val millisInFuture = blocksInFuture * blockTimeMillis

        return millisInFuture.toLong().milliseconds
    }

    override fun timestampOf(block: BlockNumber): Long {
        val offsetInBlocks = block - currentBlock
        val offsetInMillis = offsetInBlocks * blockTimeMillis

        val currentTime = System.currentTimeMillis()

        return currentTime + offsetInMillis.toLong()
    }
}
