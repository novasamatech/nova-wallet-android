@file:OptIn(ExperimentalTime::class)

package io.novafoundation.nova.feature_staking_impl.data.parachainStaking

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.asNumber
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.RoundIndex
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator.DurationCalculator.CalculationResult
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.RoundInfo
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import kotlinx.coroutines.flow.flatMapLatest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

interface RoundDurationEstimator {

    interface DurationCalculator {

        fun timeTillRound(targetRound: RoundIndex): CalculationResult

        data class CalculationResult(
            val duration: Duration,
            val calculatedAt: Long
        )
    }

    /**
     * Creates a duration calculator based on current state of the storages
     */
    suspend fun createDurationCalculator(chainId: ChainId): DurationCalculator

    suspend fun timeTillRoundFlow(chainId: ChainId, targetRound: RoundIndex): Flow<Duration>

    suspend fun unstakeDurationFlow(chainId: ChainId): Flow<Duration>

    suspend fun roundDurationFlow(chainId: ChainId): Flow<Duration>

    suspend fun firstRewardReceivingDelayFlow(chainId: ChainId): Flow<Duration>
}

class RealRoundDurationEstimator(
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val chainStateRepository: ChainStateRepository,
    private val currentRoundRepository: CurrentRoundRepository,
    private val storageDataSource: StorageDataSource
) : RoundDurationEstimator {

    override suspend fun createDurationCalculator(chainId: ChainId): RoundDurationEstimator.DurationCalculator {
        val currentRoundInfo = currentRoundRepository.currentRoundInfo(chainId)

        val blocksPerRound = currentRoundInfo.length
        val blockTime = chainStateRepository.predictedBlockTime(chainId)
        val blockNumber = chainStateRepository.currentBlock(chainId)

        return RealDurationCalculator(currentRoundInfo, blockTime, blocksPerRound, blockNumber)
    }

    override suspend fun timeTillRoundFlow(chainId: ChainId, targetRound: RoundIndex): Flow<Duration> {
        val currentRoundInfo = currentRoundRepository.currentRoundInfo(chainId)

        val blocksPerRound = currentRoundInfo.length
        val blockTime = chainStateRepository.predictedBlockTime(chainId)

        return chainStateRepository.currentBlockNumberFlow(chainId).map { currentBlock ->
            val durationCalculator = RealDurationCalculator(currentRoundInfo, blockTime, blocksPerRound, currentBlock)

            durationCalculator.timeTillRound(targetRound).duration
        }
    }

    override suspend fun unstakeDurationFlow(chainId: ChainId): Flow<Duration> {
        val bondLessDelay = parachainStakingConstantsRepository.delegationBondLessDelay(chainId)

        return estimateDuration(chainId, numberOfRounds = bondLessDelay)
    }

    override suspend fun roundDurationFlow(chainId: ChainId): Flow<Duration> {
        return combine(
            currentRoundRepository.currentRoundInfoFlow(chainId),
            chainStateRepository.predictedBlockTimeFlow(chainId)
        ) { roundInfo, blockTime ->
            val blocksPerRound = roundInfo.length

            val durationInMillis = blocksPerRound * blockTime

            durationInMillis.toLong().milliseconds
        }
    }

    override suspend fun firstRewardReceivingDelayFlow(chainId: ChainId): Flow<Duration> {
        return combine(
            observeCurrentRoundRemainingTime(chainId),
            roundDurationFlow(chainId)
        ) { remainingEraDuration, eraDuration ->
            val receivingDelayInEras = rewardReceivingDelay(chainId)
            eraDuration * receivingDelayInEras.toInt() + remainingEraDuration
        }
    }

    private suspend fun estimateDuration(chainId: ChainId, numberOfRounds: BigInteger): Flow<Duration> {
        return roundDurationFlow(chainId).map { roundDuration -> roundDuration * numberOfRounds.toInt() }
    }

    private fun observeCurrentRoundRemainingTime(chainId: String): Flow<Duration> {
        return currentRoundRepository.currentRoundInfoFlow(chainId).flatMapLatest {
            timeTillRoundFlow(chainId, it.current)
        }
    }

    private suspend fun rewardReceivingDelay(chainId: ChainId): BigInteger {
        return storageDataSource.query(chainId) {
            runtime.metadata.parachainStaking().constant("RewardPaymentDelay").asNumber(runtime)
        }
    }
}

class RealDurationCalculator(
    private val currentRoundInfo: RoundInfo,
    private val blockTime: BigInteger,
    private val blocksPerRound: BigInteger,
    private val currentBlockNumber: BlockNumber,
) : RoundDurationEstimator.DurationCalculator {

    override fun timeTillRound(targetRound: RoundIndex): CalculationResult {
        // minus one since current round is going and it is not full
        val remainedFullRounds = (targetRound - currentRoundInfo.current - BigInteger.ONE).coerceAtLeast(BigInteger.ZERO)

        val remainedBlocksTillCurrentRound = (currentRoundInfo.first + currentRoundInfo.length - currentBlockNumber).coerceAtLeast(BigInteger.ZERO)

        val remainedBlocks = remainedFullRounds * blocksPerRound + remainedBlocksTillCurrentRound
        val durationInMillis = remainedBlocks * blockTime

        return CalculationResult(
            duration = durationInMillis.toLong().milliseconds,
            calculatedAt = System.currentTimeMillis()
        )
    }
}
