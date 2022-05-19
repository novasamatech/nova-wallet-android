@file:OptIn(ExperimentalTime::class)

package io.novafoundation.nova.feature_staking_impl.data.parachainStaking

import io.novafoundation.nova.feature_staking_api.domain.model.parachain.RoundIndex
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

    suspend fun timeTillRoundFlow(chainId: ChainId, targetRound: RoundIndex): Flow<Duration>

    suspend fun unstakeDurationFlow(chainId: ChainId): Flow<Duration>

    suspend fun roundDurationFlow(chainId: ChainId): Flow<Duration>
}

class RealRoundDurationEstimator(
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val chainStateRepository: ChainStateRepository,
    private val currentRoundRepository: CurrentRoundRepository,
) : RoundDurationEstimator {

    override suspend fun timeTillRoundFlow(chainId: ChainId, targetRound: RoundIndex): Flow<Duration> {
        val currentRoundInfo = currentRoundRepository.currentRoundInfo(chainId)

        val blocksPerRound = parachainStakingConstantsRepository.defaultBlocksPerRound(chainId)
        val blockTime = chainStateRepository.predictedBlockTime(chainId)

        // minus one since current round is going and it is not full
        val remainedFullRounds = (targetRound - currentRoundInfo.current - BigInteger.ONE).coerceAtLeast(BigInteger.ZERO)

        return chainStateRepository.currentBlockNumberFlow(chainId).map { currentBlock ->
            val remainedBlocksTillCurrentRound = (currentRoundInfo.first + currentRoundInfo.length - currentBlock).coerceAtLeast(BigInteger.ZERO)

            val remainedBlocks = remainedFullRounds * blocksPerRound + remainedBlocksTillCurrentRound
            val durationInMillis = remainedBlocks * blockTime

            durationInMillis.toLong().milliseconds
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
