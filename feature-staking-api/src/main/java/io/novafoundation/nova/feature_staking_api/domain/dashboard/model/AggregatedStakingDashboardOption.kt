package io.novafoundation.nova.feature_staking_api.domain.dashboard.model

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class AggregatedStakingDashboardOption<S>(
    val chain: Chain,
    val token: Token,
    val stakingState: S,
    val syncing: Boolean
) {

    class HasStake(
        val stakingType: Chain.Asset.StakingType,
        val stake: Balance,
        val stats: ExtendedLoadingState<Stats>,
    ) {

        class Stats(val rewards: Balance, val estimatedEarnings: Percent, val status: StakingStatus)

        enum class StakingStatus {
            ACTIVE, INACTIVE, WAITING
        }
    }

    class NoStake(val stats: ExtendedLoadingState<Stats>, val flowType: FlowType) {

        sealed class FlowType {

            object Aggregated : FlowType()

            class Single(val stakingType: Chain.Asset.StakingType) : FlowType()
        }

        class Stats(val estimatedEarnings: Percent)
    }
}
