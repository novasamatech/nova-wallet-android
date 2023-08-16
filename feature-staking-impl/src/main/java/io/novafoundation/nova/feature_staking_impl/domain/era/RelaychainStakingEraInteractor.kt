package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.EraTimeCalculator
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.erasDuration
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.StartStakingEraInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

class RelaychainStakingEraInteractor(
    private val stakingSharedComputation: StakingSharedComputation,
    private val sharedComputationScope: CoroutineScope,
    private val stakingOption: StakingOption,
    private val stakingConstantsRepository: StakingConstantsRepository
) : StakingEraInteractor {

    override fun observeEraInfo(): Flow<StartStakingEraInfo> {
        val chain = stakingOption.chain

        return stakingSharedComputation.eraCalculatorFlow(stakingOption, sharedComputationScope).map { eraTimeCalculator ->
            val unstakeEras = stakingConstantsRepository.lockupPeriodInEras(chain.id)

            StartStakingEraInfo(
                unstakeTime = eraTimeCalculator.erasDuration(numberOfEras = unstakeEras),
                eraDuration = eraTimeCalculator.eraDuration(),
                firstRewardReceivingDuration = eraTimeCalculator.firstRewardDelay()
            )
        }
    }

    private fun EraTimeCalculator.firstRewardDelay(): Duration {
        return remainingEraDuration() + eraDuration()
    }
}
