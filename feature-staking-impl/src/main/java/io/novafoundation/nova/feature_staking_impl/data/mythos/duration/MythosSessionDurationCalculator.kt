package io.novafoundation.nova.feature_staking_impl.data.mythos.duration

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.toDuration
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealMythosSessionRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.EraRewardCalculatorComparable
import io.novafoundation.nova.feature_staking_impl.domain.common.ignoreInsignificantTimeChanges
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigInteger
import javax.inject.Inject
import kotlin.time.Duration

interface MythosSessionDurationCalculator : EraRewardCalculatorComparable {

    val blockTime: BigInteger

    fun sessionDuration(): Duration

    /**
     * Remaining time of the current session
     */
    fun remainingSessionDuration(): Duration
}

fun MythosSessionDurationCalculator.sessionsDuration(numberOfSessions: Int): Duration {
    return sessionDuration() * numberOfSessions
}

@FeatureScope
class MythosSessionDurationCalculatorFactory @Inject constructor(
    private val mythosSessionRepository: RealMythosSessionRepository,
    private val chainStateRepository: ChainStateRepository,
) {

    fun create(stakingOption: StakingOption): Flow<MythosSessionDurationCalculator> {
        val chainId = stakingOption.chain.id

        return flowOfAll {
            val sessionLength = mythosSessionRepository.sessionLength(stakingOption.chain)

            combine(
                mythosSessionRepository.currentSlotFlow(chainId),
                chainStateRepository.predictedBlockTimeFlow(chainId)
            ) { currentSlot, blockTime ->
                RealMythosSessionDurationCalculator(
                    blockTime = blockTime,
                    currentSlot = currentSlot,
                    slotsInSession = sessionLength
                )
            }
        }.ignoreInsignificantTimeChanges()
    }
}

private class RealMythosSessionDurationCalculator(
    override val blockTime: BigInteger,
    private val currentSlot: BigInteger,
    private val slotsInSession: BigInteger
) : MythosSessionDurationCalculator {

    override fun sessionDuration(): Duration {
        return (slotsInSession * blockTime).toDuration()
    }

    override fun remainingSessionDuration(): Duration {
        val remainingBlocks = slotsInSession - sessionProgress()
        return (remainingBlocks * blockTime).toDuration()
    }

    override fun derivedTimestamp(): Duration {
        return (currentSlot * blockTime).toDuration()
    }

    private fun sessionProgress(): BigInteger {
        // Mythos has 0 offset for sessions, so first block number of a session is divisible by session length
        return currentSlot % slotsInSession
    }
}
