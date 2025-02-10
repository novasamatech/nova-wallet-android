package io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.model.StakeSummary
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.activeStake
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.hasActiveValidators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface MythosStakeSummaryInteractor {

    context(ComputationalScope)
    fun stakeSummaryFlow(
        delegatorState: MythosDelegatorState.Staked,
        stakingOption: StakingOption,
    ): Flow<StakeSummary<MythosDelegatorStatus>>
}

@FeatureScope
class RealMythosStakeSummaryInteractor @Inject constructor(
    private val mythosSharedComputation: MythosSharedComputation,
) : MythosStakeSummaryInteractor {

    context(ComputationalScope)
    override fun stakeSummaryFlow(
        delegatorState: MythosDelegatorState.Staked,
        stakingOption: StakingOption,
    ): Flow<StakeSummary<MythosDelegatorStatus>> {
        val chainId = stakingOption.assetWithChain.chain.id

        return mythosSharedComputation.sessionValidatorsFlow(chainId).map { sessionValidators ->
            val status = when {
                delegatorState.activeStake.isZero -> MythosDelegatorStatus.Inactive
                delegatorState.hasActiveValidators(sessionValidators) -> MythosDelegatorStatus.Active
                else -> MythosDelegatorStatus.Inactive
            }

            StakeSummary(
                status = status,
                activeStake = delegatorState.activeStake
            )
        }
    }
}
