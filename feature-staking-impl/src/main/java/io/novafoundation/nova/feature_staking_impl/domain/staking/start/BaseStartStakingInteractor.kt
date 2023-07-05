package io.novafoundation.nova.feature_staking_impl.domain.staking.start

import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractor
import io.novafoundation.nova.runtime.state.assetWithChain
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

abstract class BaseStartStakingInteractor(
    internal val stakingSharedState: StakingSharedState,
    internal val stakingSharedComputation: StakingSharedComputation,
    internal val coroutineScope: CoroutineScope,
) : StartStakingInteractor {

    override fun observeMaxEarningRate(): Flow<Double> {
        return stakingSharedState.assetWithChain
            .map {
                stakingSharedComputation.rewardCalculator(stakingSharedState.selectedOption(), scope = coroutineScope)
                    .maxAPY
            }
    }
}
