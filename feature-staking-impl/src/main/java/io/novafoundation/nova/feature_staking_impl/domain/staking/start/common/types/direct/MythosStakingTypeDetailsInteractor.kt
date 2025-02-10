package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class MythosStakingTypeDetailsInteractorFactory(
    private val mythosSharedComputation: MythosSharedComputation,
) : StakingTypeDetailsInteractorFactory {

    override suspend fun create(
        stakingOption: StakingOption,
        computationalScope: ComputationalScope
    ): StakingTypeDetailsInteractor {
        return MythosStakingTypeDetailsInteractor(
            mythosSharedComputation = mythosSharedComputation,
            stakingOption = stakingOption,
            computationalScope = computationalScope
        )
    }
}

private class MythosStakingTypeDetailsInteractor(
    private val mythosSharedComputation: MythosSharedComputation,
    private val stakingOption: StakingOption,
    computationalScope: ComputationalScope
) : StakingTypeDetailsInteractor, ComputationalScope by computationalScope {

    override fun observeData(): Flow<StakingTypeDetails> {
        return flowOfAll {
            val chain = stakingOption.chain

            val rewardCalculator = mythosSharedComputation.rewardCalculator(chain.id)

            mythosSharedComputation.minStakeFlow(chain.id).map { minStake ->
                StakingTypeDetails(
                    maxEarningRate = rewardCalculator.maxApr,
                    minStake = minStake,
                    payoutType = PayoutType.Manual,
                    participationInGovernance = false,
                    advancedOptionsAvailable = false,
                    stakingType = stakingOption.stakingType
                )
            }
        }
    }

    override suspend fun getAvailableBalance(asset: Asset): BigInteger {
        return asset.freeInPlanks
    }
}
