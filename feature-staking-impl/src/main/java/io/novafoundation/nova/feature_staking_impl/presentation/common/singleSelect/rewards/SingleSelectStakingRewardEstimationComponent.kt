package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.rewards

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_staking_impl.domain.rewards.PeriodReturns
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapPeriodReturnsToRewardEstimation
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.RewardEstimation
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import java.math.BigDecimal

abstract class SingleSelectStakingRewardEstimationComponent(
    private val resourceManager: ResourceManager,

    computationalScope: ComputationalScope,
    assetFlow: Flow<Asset>,
    selectedAmountFlow: Flow<BigDecimal>,
    selectedTargetFlow: Flow<AccountIdKey?>
): StakingRewardEstimationComponent,
    ComputationalScope by computationalScope {

    abstract suspend fun calculatePeriodReturns(selectedTarget: AccountIdKey?, selectedAmount: BigDecimal): PeriodReturns

    private val periodReturnsFlow = combine(
        selectedAmountFlow.onStart { emit(BigDecimal.ONE) },
        selectedTargetFlow.onStart { emit(null) }
    ) { selectedAmount, selectedTarget ->
        calculatePeriodReturns(selectedTarget, selectedAmount)
    }

    override val rewardEstimation: Flow<RewardEstimation> = combine(assetFlow, periodReturnsFlow) { asset, periodReturns ->
         mapPeriodReturnsToRewardEstimation(
            periodReturns = periodReturns,
            token = asset.token,
            resourceManager = resourceManager,
        )
    }.shareInBackground()
}
