package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.fractions
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.components
import io.novafoundation.nova.feature_staking_impl.data.stakingType
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.model.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetails
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class RelaychainStakingTypeDetailsInteractorFactory(
    private val stakingSharedComputation: StakingSharedComputation,
) : StakingTypeDetailsInteractorFactory {

    override suspend fun create(
        stakingOption: StakingOption,
        computationalScope: ComputationalScope
    ): StakingTypeDetailsInteractor {
        return RelaychainStakingTypeDetailsInteractor(
            stakingSharedComputation,
            stakingOption,
            computationalScope,
        )
    }
}

class RelaychainStakingTypeDetailsInteractor(
    private val stakingSharedComputation: StakingSharedComputation,
    private val stakingOption: StakingOption,
    private val computationalScope: ComputationalScope,
) : StakingTypeDetailsInteractor {

    override fun observeData(): Flow<StakingTypeDetails> {
        val chain = stakingOption.chain

        return stakingSharedComputation.activeEraInfo(chain.id, computationalScope).map { activeEraInfo ->
            StakingTypeDetails(
                maxEarningRate = calculateEarningRate(),
                minStake = activeEraInfo.minStake,
                payoutType = PayoutType.Automatically.Restake,
                participationInGovernance = chain.governance.isNotEmpty(),
                advancedOptionsAvailable = true,
                stakingType = stakingOption.stakingType
            )
        }
    }

    override suspend fun getAvailableBalance(asset: Asset): BigInteger {
        return asset.freeInPlanks
    }

    private suspend fun calculateEarningRate(): Fraction {
        val (chain, chainAsset, stakingType) = stakingOption.components

        return stakingSharedComputation.rewardCalculator(chain, chainAsset, stakingType, computationalScope)
            .maxAPY
            .fractions
    }
}
