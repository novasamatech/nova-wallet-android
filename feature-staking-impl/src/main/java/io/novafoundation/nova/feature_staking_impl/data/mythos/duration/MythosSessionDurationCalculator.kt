package io.novafoundation.nova.feature_staking_impl.data.mythos.duration

import io.novafoundation.nova.common.utils.toDuration
import java.math.BigInteger
import kotlin.time.Duration

interface MythosSessionDurationCalculator {

    fun sessionDuration(): Duration

    /**
     * Remaining time of the current session
     */
    fun remainingSessionDuration(): Duration
}

class RealMythosSessionDurationCalculator(
    private val blockTime: BigInteger,
    private val currentSlot: BigInteger,
    private val slotsInSession: BigInteger
): MythosSessionDurationCalculator {

    override fun sessionDuration(): Duration {
        return (slotsInSession * blockTime).toDuration()
    }

    override fun remainingSessionDuration(): Duration {
       return (slotsInSession - sessionProgress()).toDuration()
    }

    private fun sessionProgress(): BigInteger {
        // Mythos has 0 offset for sessions, so first block number of a session is divisible by session length
        return currentSlot % slotsInSession
    }
}
