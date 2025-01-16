package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.model.StartStakingEraInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration

class MythosStakingEraInteractor(
    private val stakingOption: StakingOption,
    private val computationalScope: ComputationalScope
): StakingEraInteractor {

    override fun observeEraInfo(): Flow<StartStakingEraInfo> {
       return flowOf(
           StartStakingEraInfo(
               unstakeTime = Duration.ZERO,
               eraDuration = Duration.ZERO,
               firstRewardReceivingDuration = Duration.ZERO
           )
       )
    }
}
