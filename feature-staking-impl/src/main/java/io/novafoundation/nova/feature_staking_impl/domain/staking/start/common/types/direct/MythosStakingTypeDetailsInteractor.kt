package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope

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
        val chain = stakingOption.chain

        return mythosSharedComputation.minStakeFlow(chain.id).map { minStake ->
            StakingTypeDetails(
                // TODO mythos reward calculator
                maxEarningRate = Fraction.ZERO,
                minStake = minStake,
                payoutType = PayoutType.Manual,
                participationInGovernance = false,
                advancedOptionsAvailable = false,
                stakingType = stakingOption.stakingType
            )
        }
    }

    override suspend fun getAvailableBalance(asset: Asset): BigInteger {
        return asset.freeInPlanks
    }
}
