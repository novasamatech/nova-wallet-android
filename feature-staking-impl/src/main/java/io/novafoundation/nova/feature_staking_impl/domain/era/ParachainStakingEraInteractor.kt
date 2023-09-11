package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.model.StartStakingEraInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ParachainStakingEraInteractor(
    private val roundDurationEstimator: RoundDurationEstimator,
    private val stakingOption: StakingOption,
) : StakingEraInteractor {

    override fun observeEraInfo(): Flow<StartStakingEraInfo> {
        val chain = stakingOption.chain

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
