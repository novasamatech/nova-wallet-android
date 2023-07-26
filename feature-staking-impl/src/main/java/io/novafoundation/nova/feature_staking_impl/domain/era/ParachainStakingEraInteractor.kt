package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.common.utils.asNumber
import io.novafoundation.nova.common.utils.constant
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.StartStakingEraInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlin.time.Duration

class ParachainStakingEraInteractor(
    private val roundDurationEstimator: RoundDurationEstimator,
    private val currentRoundRepository: CurrentRoundRepository,
    private val storageDataSource: StorageDataSource
) : StakingEraInteractor {

    override fun observeEraInfo(chain: Chain): Flow<StartStakingEraInfo> {
        return flowOfAll {
            combine(
                observeCurrentRoundRemainingTime(chain.id),
                roundDurationEstimator.unstakeDurationFlow(chain.id),
                roundDurationEstimator.roundDurationFlow(chain.id)
            ) { remainingEraDuration, unstakeDuration, eraDuration ->
                val receivingDelayInEras = getRewardReceivingDelay(chain)
                val firstRewardReceivingDelay = eraDuration * receivingDelayInEras.toInt() + remainingEraDuration
                StartStakingEraInfo(remainingEraDuration, unstakeDuration, eraDuration, firstRewardReceivingDelay)
            }
        }
    }

    private fun observeCurrentRoundRemainingTime(chainId: String): Flow<Duration> {
        return currentRoundRepository.currentRoundInfoFlow(chainId).flatMapLatest {
            roundDurationEstimator.timeTillRoundFlow(chainId, it.current)
        }
    }

    private suspend fun getRewardReceivingDelay(chain: Chain): BigInteger {
        return storageDataSource.query(
            chain.id
        ) {
            runtime.metadata.parachainStaking().constant("RewardPaymentDelay").asNumber(runtime)
        }
    }
}
