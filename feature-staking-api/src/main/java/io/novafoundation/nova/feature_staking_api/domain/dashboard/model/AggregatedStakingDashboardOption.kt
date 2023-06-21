package io.novafoundation.nova.feature_staking_api.domain.dashboard.model

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.SyncingStage
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class AggregatedStakingDashboardOption<out S>(
    val chain: Chain,
    val token: Token,
    val stakingState: S,
    val syncingStage: SyncingStage
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

    sealed interface WithoutStake

    class NoStake(val stats: ExtendedLoadingState<Stats>, val flowType: FlowType, val availableBalance: Balance) : WithoutStake {

        sealed class FlowType {

            class Aggregated(val stakingTypes: List<Chain.Asset.StakingType>) : FlowType()

            class Single(val stakingType: Chain.Asset.StakingType) : FlowType()
        }

        class Stats(val estimatedEarnings: Percent)
    }

    object NotYetResolved : WithoutStake

    enum class SyncingStage {
        SYNCING_ALL, SYNCING_SECONDARY, SYNCED
    }
}

fun SyncingStage.isSyncing(): Boolean {
    return this != SyncingStage.SYNCED
}

fun SyncingStage.isSyncingPrimary(): Boolean {
    return this == SyncingStage.SYNCING_ALL
}

fun SyncingStage.isSyncingSecondary(): Boolean {
    return this < SyncingStage.SYNCED
}
