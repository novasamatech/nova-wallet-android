package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.rewards.PeriodReturns
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.rewards.SingleSelectStakingRewardEstimationComponent
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.rewards.SingleSelectStakingRewardEstimationComponentFactory
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

class ParachainStakingRewardsComponentFactory(
    private val rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val singleAssetSharedState: StakingSharedState,
    private val resourceManager: ResourceManager,
) : SingleSelectStakingRewardEstimationComponentFactory {

    override fun create(
        computationalScope: ComputationalScope,
        assetFlow: Flow<Asset>,
        selectedAmount: Flow<BigDecimal>,
        selectedTarget: Flow<AccountIdKey?>
    ): SingleSelectStakingRewardEstimationComponent = ParachainStakingRewardsComponent(
        rewardCalculatorFactory = rewardCalculatorFactory,
        singleAssetSharedState = singleAssetSharedState,
        resourceManager = resourceManager,
        computationalScope = computationalScope,
        assetFlow = assetFlow,
        selectedAmountFlow = selectedAmount,
        selectedTargetFlow = selectedTarget
    )
}

private class ParachainStakingRewardsComponent(
    private val rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val singleAssetSharedState: StakingSharedState,
    resourceManager: ResourceManager,

    computationalScope: ComputationalScope,
    assetFlow: Flow<Asset>,
    selectedAmountFlow: Flow<BigDecimal>,
    selectedTargetFlow: Flow<AccountIdKey?>
) : SingleSelectStakingRewardEstimationComponent(resourceManager, computationalScope, assetFlow, selectedAmountFlow, selectedTargetFlow) {

    private val rewardCalculator by lazyAsync {
        rewardCalculatorFactory.create(singleAssetSharedState.selectedOption())
    }

    override suspend fun calculatePeriodReturns(selectedTarget: AccountIdKey?, selectedAmount: BigDecimal): PeriodReturns {
        return if (selectedTarget != null) {
            rewardCalculator().calculateCollatorAnnualReturns(selectedTarget.value, selectedAmount)
        } else {
            rewardCalculator().calculateMaxAnnualReturns(selectedAmount)
        }
    }
}
