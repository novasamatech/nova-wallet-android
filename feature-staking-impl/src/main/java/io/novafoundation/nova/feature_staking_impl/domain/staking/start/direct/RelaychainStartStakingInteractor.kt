package io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.components
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.rewards.DAYS_IN_YEAR
import io.novafoundation.nova.feature_staking_impl.domain.rewards.calculateMaxPeriodReturns
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.BigInteger

class RelaychainStartStakingInteractor(
    private val stakingSharedComputation: StakingSharedComputation,
    private val stakingOption: StakingOption,
    private val coroutineScope: CoroutineScope,
) : StartStakingInteractor {

    override fun observeData(): Flow<StartStakingData> {
        val chain = stakingOption.chain

        return stakingSharedComputation.activeEraInfo(chain.id, coroutineScope).map { activeEraInfo ->
            StartStakingData(
                maxEarningRate = calculateEarningRate(),
                minStake = activeEraInfo.minStake,
                payoutType = PayoutType.Automatic.Restake,
                participationInGovernance = chain.governance.isNotEmpty()
            )
        }
    }

    override fun getAvailableBalance(asset: Asset): BigInteger {
        return asset.freeInPlanks
    }

    private suspend fun calculateEarningRate(): BigDecimal {
        val (chain, chainAsset, stakingType) = stakingOption.components

        return stakingSharedComputation.rewardCalculator(chain, chainAsset, stakingType, coroutineScope)
            .calculateMaxPeriodReturns(DAYS_IN_YEAR)
    }
}
