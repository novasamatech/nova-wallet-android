package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.toDuration
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.mythos.duration.MythosSessionDurationCalculator
import io.novafoundation.nova.feature_staking_impl.data.mythos.duration.sessionsDuration
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.model.StartStakingEraInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

class MythosStakingEraInteractor(
    private val mythosSharedComputation: MythosSharedComputation,
    private val mythosStakingRepository: MythosStakingRepository,
    private val stakingOption: StakingOption,
    private val computationalScope: ComputationalScope
) : StakingEraInteractor, ComputationalScope by computationalScope {

    override fun observeEraInfo(): Flow<StartStakingEraInfo> {
        return flowOfAll {
            val chainId = stakingOption.chain.id
            val unstakingDurationInBlocks = mythosStakingRepository.unstakeDurationInBlocks(chainId)

            mythosSharedComputation.eraDurationCalculatorFlow(stakingOption).map { sessionDurationCalculator ->
                StartStakingEraInfo(
                    unstakeTime = (unstakingDurationInBlocks * sessionDurationCalculator.blockTime).toDuration(),
                    eraDuration = sessionDurationCalculator.sessionDuration(),
                    firstRewardReceivingDuration = sessionDurationCalculator.firstRewardDelay()
                )
            }
        }
    }

    private fun MythosSessionDurationCalculator.firstRewardDelay(): Duration {
        return remainingSessionDuration() + sessionDuration()
    }
}
