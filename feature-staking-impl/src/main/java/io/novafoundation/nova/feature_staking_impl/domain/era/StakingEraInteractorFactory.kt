package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.createStakingOption
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.extensions.tryFindNonNull
import kotlinx.coroutines.CoroutineScope

class StakingEraInteractorFactory(
    private val roundDurationEstimator: RoundDurationEstimator,
    private val stakingSharedComputation: StakingSharedComputation,
    private val stakingConstantsRepository: StakingConstantsRepository,
) {

    private val creators = mapOf(
        StakingTypeGroup.RELAYCHAIN to ::createRelaychain,
        StakingTypeGroup.PARACHAIN to ::createParachain
    )

    fun create(chain: Chain, chainAsset: Chain.Asset, sharedComputationScope: CoroutineScope): StakingEraInteractor {
        return creators.entries.tryFindNonNull { (stakingTypeGroup, creator) ->
            val stakingType = chainAsset.findStakingTypeByGroup(stakingTypeGroup) ?: return@tryFindNonNull null
            val stakingOption = createStakingOption(chain, chainAsset, stakingType)

            creator(stakingOption, sharedComputationScope)
        } ?: UnsupportedStakingEraInteractor()
    }

    private fun Chain.Asset.findStakingTypeByGroup(stakingTypeGroup: StakingTypeGroup): Chain.Asset.StakingType? {
        return staking.find { it.group() == stakingTypeGroup }
    }

    private fun createParachain(stakingOption: StakingOption, sharedComputationScope: CoroutineScope): StakingEraInteractor {
        return ParachainStakingEraInteractor(
            roundDurationEstimator = roundDurationEstimator,
            stakingOption = stakingOption
        )
    }

    private fun createRelaychain(stakingOption: StakingOption, sharedComputationScope: CoroutineScope): StakingEraInteractor {
        return RelaychainStakingEraInteractor(
            stakingSharedComputation = stakingSharedComputation,
            sharedComputationScope = sharedComputationScope,
            stakingOption = stakingOption,
            stakingConstantsRepository = stakingConstantsRepository
        )
    }
}
