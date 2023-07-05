package io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.BaseStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.PayoutType
import io.novafoundation.nova.runtime.state.assetWithChain
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class RelaychainStartStakingInteractor(
    stakingSharedState: StakingSharedState,
    stakingSharedComputation: StakingSharedComputation,
    coroutineScope: CoroutineScope
) : BaseStartStakingInteractor(stakingSharedState, stakingSharedComputation, coroutineScope) {

    override fun observeMinStake(): Flow<BigInteger> {
        return stakingSharedState.assetWithChain
            .flatMapLatest {
                stakingSharedComputation.activeEraInfo(it.chain.id, coroutineScope)
            }.map { it.minStake }
    }

    override fun observePayoutType(): Flow<PayoutType> {
        return flowOf { PayoutType.Automatic.Restake }
    }

    override fun observeParticipationInGovernance(): Flow<Boolean> {
        return stakingSharedState.assetWithChain.map { it.chain.governance.isNotEmpty() }
    }
}
