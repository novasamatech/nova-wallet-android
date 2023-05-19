package io.novafoundation.nova.feature_staking_impl.data.dashboard.model

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class StakingDashboardItem(
    val fullChainAssetId: FullChainAssetId,
    val stakingType: Chain.Asset.StakingType,
    val stakeState: StakeState,
) {

    sealed interface StakeState {

        val stats: ExtendedLoadingState<CommonStats>

        class HasStake(
            val stake: Balance,
            override val stats: ExtendedLoadingState<Stats>,
        ) : StakeState {

            class Stats(
                val rewards: Balance,
                val status: StakingStatus,
                override val estimatedEarnings: Percent
            ) : CommonStats

            enum class StakingStatus {
                ACTIVE, INACTIVE, WAITING
            }
        }

        class NoStake(override val stats: ExtendedLoadingState<Stats>) : StakeState {

            class Stats(override val estimatedEarnings: Percent) : CommonStats
        }

        interface CommonStats {

            val estimatedEarnings: Percent
        }
    }
}
