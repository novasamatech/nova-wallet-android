package io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct

import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.BaseStartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.PayoutType
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.state.assetWithChain
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class ParachainStartStakingInteractor(
    stakingSharedState: StakingSharedState,
    stakingSharedComputation: StakingSharedComputation,
    coroutineScope: CoroutineScope,
    private val parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor
) : BaseStartStakingInteractor(stakingSharedState, stakingSharedComputation, coroutineScope) {

    override fun observeMinStake(): Flow<BigInteger> {
        return stakingSharedState.assetWithChain
            .flatMapLatest { parachainNetworkInfoInteractor.observeRoundInfo(it.chain.id) }
            .map { it.minimumStake }
    }

    override fun observePayoutType(): Flow<PayoutType> {
        return stakingSharedState.assetWithChain
            .map {
                when (it.chain.id) {
                    ChainGeneses.MOONBEAM -> PayoutType.Automatic.Payout
                    else -> PayoutType.Manual
                }
            }
    }
}
