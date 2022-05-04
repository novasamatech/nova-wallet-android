@file:OptIn(ExperimentalTime::class)

package io.novafoundation.nova.feature_staking_impl.data.parachainStaking

import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

interface RoundDurationEstimator {

    suspend fun estimateDuration(chainId: ChainId, numberOfRounds: BigInteger): Duration

    suspend fun unstakeDuration(chainId: ChainId): Duration
}

class RealRoundDurationEstimator(
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val chainStateRepository: ChainStateRepository,
) : RoundDurationEstimator {

    override suspend fun estimateDuration(chainId: ChainId, numberOfRounds: BigInteger): Duration {
        val blocksPerRound = parachainStakingConstantsRepository.defaultBlocksPerRound(chainId)
        val blockTime = chainStateRepository.predictedBlockTime(chainId)

        val durationInMillis = numberOfRounds * blocksPerRound * blockTime

        return durationInMillis.toLong().milliseconds
    }

    override suspend fun unstakeDuration(chainId: ChainId): Duration {
        val bondLessDelay = parachainStakingConstantsRepository.delegationBondLessDelay(chainId)

        return estimateDuration(chainId, numberOfRounds = bondLessDelay)
    }
}
