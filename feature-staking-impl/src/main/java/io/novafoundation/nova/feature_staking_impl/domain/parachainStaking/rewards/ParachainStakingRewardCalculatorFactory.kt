package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards

import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.RewardsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.repository.TuringStakingRewardsRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.TURING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository

class ParachainStakingRewardCalculatorFactory(
    private val rewardsRepository: RewardsRepository,
    private val currentRoundRepository: CurrentRoundRepository,
    private val commonStakingRepository: TotalIssuanceRepository,
    private val turingStakingRewardsRepository: TuringStakingRewardsRepository,
) {

    suspend fun create(
        chainAsset: Chain.Asset,
        snapshots: AccountIdMap<CollatorSnapshot>
    ): ParachainStakingRewardCalculator {
        val chainId = chainAsset.chainId

        // TODO staking dashboard - switch by selected staking option
        return when (chainAsset.staking.firstOrNull()) {
            PARACHAIN -> defaultCalculator(chainId, snapshots)
            TURING -> turingCalculator(chainId, snapshots)
            null, RELAYCHAIN, RELAYCHAIN_AURA, ALEPH_ZERO, UNSUPPORTED -> {
                throw IllegalStateException("Unknown staking type in ParachainStakingRewardCalculatorFactory")
            }
        }
    }

    private suspend fun turingCalculator(
        chainId: ChainId,
        snapshots: AccountIdMap<CollatorSnapshot>
    ): ParachainStakingRewardCalculator {
        val additionalIssuance = turingStakingRewardsRepository.additionalIssuance(chainId)
        val totalIssuance = commonStakingRepository.getTotalIssuance(chainId)

        val circulating = additionalIssuance + totalIssuance

        return RealParachainStakingRewardCalculator(
            bondConfig = rewardsRepository.getParachainBondConfig(chainId),
            inflationInfo = rewardsRepository.getInflationInfo(chainId),
            totalIssuance = circulating,
            totalStaked = currentRoundRepository.totalStaked(chainId),
            collators = snapshots.toCollatorList(),
            collatorCommission = rewardsRepository.getCollatorCommission(chainId)
        )
    }

    private suspend fun defaultCalculator(
        chainId: ChainId,
        snapshots: AccountIdMap<CollatorSnapshot>
    ) = RealParachainStakingRewardCalculator(
        bondConfig = rewardsRepository.getParachainBondConfig(chainId),
        inflationInfo = rewardsRepository.getInflationInfo(chainId),
        totalIssuance = commonStakingRepository.getTotalIssuance(chainId),
        totalStaked = currentRoundRepository.totalStaked(chainId),
        collators = snapshots.toCollatorList(),
        collatorCommission = rewardsRepository.getCollatorCommission(chainId)
    )

    suspend fun create(chainAsset: Chain.Asset): ParachainStakingRewardCalculator {
        val chainId = chainAsset.chainId

        val roundIndex = currentRoundRepository.currentRoundInfo(chainId).current
        val snapshot = currentRoundRepository.collatorsSnapshot(chainId, roundIndex)

        return create(chainAsset, snapshot)
    }

    private fun AccountIdMap<CollatorSnapshot>.toCollatorList() = entries.map { (accountIdHex, snapshot) ->
        ParachainStakingRewardTarget(
            totalStake = snapshot.total,
            accountIdHex = accountIdHex
        )
    }
}
