package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class StakingEraInteractorFactory(
    private val roundDurationEstimator: RoundDurationEstimator,
    private val currentRoundRepository: CurrentRoundRepository,
    private val stakingInteractor: StakingInteractor
) {

    fun create(chainAsset: Chain.Asset): StakingEraInteractor {
        return when {
            chainAsset.hasStakingTypeGroup(StakingTypeGroup.PARACHAIN) -> ParachainStakingEraInteractor(roundDurationEstimator, currentRoundRepository)
            chainAsset.hasStakingTypeGroup(StakingTypeGroup.RELAYCHAIN) -> RelaychainStakingEraInteractor(stakingInteractor)
            else -> UnsupportedStakingEraInteractor()
        }
    }

    private fun Chain.Asset.hasStakingTypeGroup(stakingTypeGroup: StakingTypeGroup): Boolean {
        return staking.any { it.group() == stakingTypeGroup }
    }
}
