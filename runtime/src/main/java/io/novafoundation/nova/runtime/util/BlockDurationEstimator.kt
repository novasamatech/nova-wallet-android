package io.novafoundation.nova.runtime.util

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.formatting.toTimerValue
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface BlockDurationEstimator {

    val currentBlock: BlockNumber

    fun durationUntil(block: BlockNumber): Duration

    fun durationOf(blocks: BlockNumber): Duration

    fun timestampOf(block: BlockNumber): Long

    fun blockInFuture(duration: Duration): BlockNumber
}

fun BlockDurationEstimator.blockInPast(duration: Duration): BlockNumber {
    return blockInFuture(-duration)
}

fun BlockDurationEstimator.timerUntil(block: BlockNumber): TimerValue {
    return durationUntil(block).toTimerValue()
}

fun BlockDurationEstimator.isBlockedPassed(block: BlockNumber): Boolean {
    return currentBlock >= block
}

fun BlockDurationEstimator(currentBlock: BlockNumber, blockTimeMillis: BigInteger): BlockDurationEstimator {
    return RealBlockDurationEstimator(currentBlock, blockTimeMillis)
}

internal class RealBlockDurationEstimator(
    override val currentBlock: BlockNumber,
    private val blockTimeMillis: BigInteger
) : BlockDurationEstimator {

    override fun durationUntil(block: BlockNumber): Duration {
        val blocksInFuture = block - currentBlock

        return durationOf(blocksInFuture)
    }

    override fun durationOf(blocks: BlockNumber): Duration {
        if (blocks < BigInteger.ZERO) return Duration.ZERO

        val millisInFuture = blocks * blockTimeMillis

        return millisInFuture.toLong().milliseconds
    }

    override fun timestampOf(block: BlockNumber): Long {
        val offsetInBlocks = block - currentBlock
        val offsetInMillis = offsetInBlocks * blockTimeMillis

        val currentTime = System.currentTimeMillis()

        return currentTime + offsetInMillis.toLong()
    }

    override fun blockInFuture(duration: Duration): BlockNumber {
        val offsetInBlocks = duration.inWholeMilliseconds.toBigInteger() / blockTimeMillis

        return currentBlock + offsetInBlocks
    }
}
