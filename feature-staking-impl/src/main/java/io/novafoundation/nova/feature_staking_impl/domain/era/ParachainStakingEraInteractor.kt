package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.StartStakingEraInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ParachainStakingEraInteractor(
    private val roundDurationEstimator: RoundDurationEstimator
) : StakingEraInteractor {

    override fun observeEraInfo(chain: Chain): Flow<StartStakingEraInfo> {
        return flowOfAll {
            combine(
                roundDurationEstimator.unstakeDurationFlow(chain.id),
                roundDurationEstimator.roundDurationFlow(chain.id),
                roundDurationEstimator.firstRewardReceivingDelayFlow(chain.id)
            ) { unstakeDuration, eraDuration, firstRewardReceivingDelay ->
                StartStakingEraInfo(unstakeDuration, eraDuration, firstRewardReceivingDelay)
            }
        }
    }
}
