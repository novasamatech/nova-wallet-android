@file:OptIn(ExperimentalTime::class)

package io.novafoundation.nova.feature_staking_impl.data.parachainStaking

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.RoundIndex
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator.DurationCalculator.CalculationResult
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.RoundInfo
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

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
}

class RealRoundDurationEstimator(
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val chainStateRepository: ChainStateRepository,
    private val currentRoundRepository: CurrentRoundRepository,
) : RoundDurationEstimator {

    override suspend fun createDurationCalculator(chainId: ChainId): RoundDurationEstimator.DurationCalculator {
        val currentRoundInfo = currentRoundRepository.currentRoundInfo(chainId)

        val blocksPerRound = parachainStakingConstantsRepository.defaultBlocksPerRound(chainId)
        val blockTime = chainStateRepository.predictedBlockTime(chainId)
        val blockNumber = chainStateRepository.currentBlock(chainId)

        return RealDurationCalculator(currentRoundInfo, blockTime, blocksPerRound, blockNumber)
    }

    override suspend fun timeTillRoundFlow(chainId: ChainId, targetRound: RoundIndex): Flow<Duration> {
        val currentRoundInfo = currentRoundRepository.currentRoundInfo(chainId)

        val blocksPerRound = parachainStakingConstantsRepository.defaultBlocksPerRound(chainId)
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
        return chainStateRepository.predictedBlockTimeFlow(chainId).map { blockTime ->
            val blocksPerRound = parachainStakingConstantsRepository.defaultBlocksPerRound(chainId)

            val durationInMillis = blocksPerRound * blockTime

            durationInMillis.toLong().milliseconds
        }
    }

    private suspend fun estimateDuration(chainId: ChainId, numberOfRounds: BigInteger): Flow<Duration> {
        return chainStateRepository.predictedBlockTimeFlow(chainId).map { blockTime ->
            val blocksPerRound = parachainStakingConstantsRepository.defaultBlocksPerRound(chainId)

            val durationInMillis = numberOfRounds * blocksPerRound * blockTime

            durationInMillis.toLong().milliseconds
        }
    }
}

class RealDurationCalculator(
    private val currentRoundInfo: RoundInfo,
    private val blockTime: BigInteger,
    private val blocksPerRound: BigInteger,
    private val currentBlockNumber: BlockNumber,
): RoundDurationEstimator.DurationCalculator {

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
