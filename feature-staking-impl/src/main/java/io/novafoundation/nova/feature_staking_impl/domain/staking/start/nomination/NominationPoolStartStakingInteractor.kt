package io.novafoundation.nova.feature_staking_impl.domain.staking.start.nomination

import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.BaseStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.PayoutType
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NominationPoolStartStakingInteractor(
    stakingSharedState: StakingSharedState,
    stakingSharedComputation: StakingSharedComputation,
    coroutineScope: CoroutineScope
) : BaseStartStakingInteractor(stakingSharedState, stakingSharedComputation, coroutineScope) {

    override fun observeMinStake(): Flow<BigInteger> {
        return flowOf(BigInteger.ZERO)
    }

    override fun observeParticipationInGovernance(): Flow<Boolean> {
        return flowOf(false)
    }

    override fun observePayoutType(): Flow<PayoutType> {
        return flowOf(PayoutType.Manual)
    }
}
