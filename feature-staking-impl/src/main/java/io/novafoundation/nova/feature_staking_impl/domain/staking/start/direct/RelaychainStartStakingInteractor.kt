package io.novafoundation.nova.feature_staking_impl.domain.staking.start.direct

import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.rewards.DAYS_IN_YEAR
import io.novafoundation.nova.feature_staking_impl.domain.rewards.calculateMaxPeriodReturns
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingData
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.PayoutType
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RelaychainStartStakingInteractor(
    private val stakingSharedComputation: StakingSharedComputation,
    private val stakingType: Chain.Asset.StakingType,
    private val coroutineScope: CoroutineScope,
) : StartStakingInteractor {

    override fun observeData(chain: Chain, asset: Asset): Flow<StartStakingData> {
        return stakingSharedComputation.activeEraInfo(chain.id, coroutineScope).map { activeEraInfo ->
            StartStakingData(
                maxEarningRate = calculateEarningRate(chain, asset.token.configuration, stakingType),
                minStake = activeEraInfo.minStake,
                payoutType = PayoutType.Automatic.Restake,
                participationInGovernance = chain.governance.isNotEmpty()
            )
        }
    }

    override fun getAvailableBalance(asset: Asset): BigInteger {
        return asset.freeInPlanks
    }

    private suspend fun calculateEarningRate(chain: Chain, chainAsset: Chain.Asset, stakingType: Chain.Asset.StakingType): BigDecimal {
        return stakingSharedComputation.rewardCalculator(chain, chainAsset, stakingType, scope = coroutineScope)
            .calculateMaxPeriodReturns(DAYS_IN_YEAR)
    }
}
