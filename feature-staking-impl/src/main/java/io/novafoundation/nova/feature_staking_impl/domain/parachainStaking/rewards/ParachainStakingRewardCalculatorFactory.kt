package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards

import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
import io.novafoundation.nova.feature_staking_impl.data.common.repository.CommonStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.RewardsRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class ParachainStakingRewardCalculatorFactory(
    private val rewardsRepository: RewardsRepository,
    private val currentRoundRepository: CurrentRoundRepository,
    private val commonStakingRepository: CommonStakingRepository,
) {

    suspend fun create(chainId: ChainId, snapshots: AccountIdMap<CollatorSnapshot>): ParachainStakingRewardCalculator {
        val collators = snapshots.entries.map { (accountIdHex, snapshot) ->
            ParachainStakingRewardTarget(
                totalStake = snapshot.total,
                accountIdHex = accountIdHex
            )
        }

        return RealParachainStakingRewardCalculator(
            bondConfig = rewardsRepository.getParachainBondConfig(chainId),
            inflationInfo = rewardsRepository.getInflationInfo(chainId),
            totalIssuance = commonStakingRepository.getTotalIssuance(chainId),
            totalStaked = currentRoundRepository.totalStaked(chainId),
            collators = collators,
            collatorCommission = rewardsRepository.getCollatorCommission(chainId)
        )
    }

    suspend fun create(chainId: ChainId): ParachainStakingRewardCalculator {
        val roundIndex = currentRoundRepository.currentRoundInfo(chainId).current
        val snapshot = currentRoundRepository.collatorsSnapshot(chainId, roundIndex)

        return create(chainId, snapshot)
    }
}
