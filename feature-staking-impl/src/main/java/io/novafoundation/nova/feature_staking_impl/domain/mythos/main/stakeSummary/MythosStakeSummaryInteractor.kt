package io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.hasActiveValidators
import io.novafoundation.nova.feature_staking_impl.domain.model.StakeSummary
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface MythosStakeSummaryInteractor {

    context(ComputationalScope)
    fun stakeSummaryFlow(
        userStakeInfo: UserStakeInfo,
        stakingOption: StakingOption,
    ): Flow<StakeSummary<MythosDelegatorStatus>>
}

@FeatureScope
class RealMythosStakeSummaryInteractor @Inject constructor(
    private val mythosSharedComputation: MythosSharedComputation,
) : MythosStakeSummaryInteractor {

    context(ComputationalScope)
    override fun stakeSummaryFlow(
        userStakeInfo: UserStakeInfo,
        stakingOption: StakingOption,
    ): Flow<StakeSummary<MythosDelegatorStatus>> {
        val chainId = stakingOption.assetWithChain.chain.id

        return mythosSharedComputation.sessionValidatorsFlow(chainId).map { sessionValidators ->
            val status = when {
                userStakeInfo.hasActiveValidators(sessionValidators) -> MythosDelegatorStatus.Active
                else -> MythosDelegatorStatus.Inactive
            }

            StakeSummary(
                status = status,
                activeStake = userStakeInfo.balance
            )
        }
    }
}
