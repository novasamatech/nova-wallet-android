package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.runtime.state.assetWithChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlin.time.Duration

class ParachainStakingEraInteractor(
    private val stakingSharedState: StakingSharedState,
    private val roundDurationEstimator: RoundDurationEstimator,
    private val currentRoundRepository: CurrentRoundRepository,
) : StakingEraInteractor {

    override fun observeRemainingEraTime(): Flow<Duration> {
        return stakingSharedState.assetWithChain.flatMapLatest {
            observeCurrentRoundRemainingTime(it.chain.id)
        }
    }

    override fun observeUnstakeTime(): Flow<Duration> {
        return stakingSharedState.assetWithChain.flatMapLatest {
            roundDurationEstimator.unstakeDurationFlow(it.chain.id)
        }
    }

    override fun observeEraDuration(): Flow<Duration> {
        return stakingSharedState.assetWithChain.flatMapLatest {
            roundDurationEstimator.roundDurationFlow(it.chain.id)
        }
    }

    private fun observeCurrentRoundRemainingTime(chainId: String): Flow<Duration> {
        return currentRoundRepository.currentRoundInfoFlow(chainId).flatMapLatest {
            roundDurationEstimator.timeTillRoundFlow(chainId, it.current)
        }
    }
}
