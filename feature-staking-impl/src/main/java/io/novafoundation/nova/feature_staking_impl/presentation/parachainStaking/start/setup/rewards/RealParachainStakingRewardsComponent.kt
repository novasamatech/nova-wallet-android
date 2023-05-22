package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.RewardSuffix
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapPeriodReturnsToRewardEstimation
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.ParachainStakingRewardsComponent.Action
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.ParachainStakingRewardsComponent.RewardsConfiguration
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.ParachainStakingRewardsComponent.State
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class RealParachainStakingRewardsComponentFactory(
    private val rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val singleAssetSharedState: AnySelectedAssetOptionSharedState,
    private val resourceManager: ResourceManager,
) {

    fun create(
        parentScope: CoroutineScope,
        assetFlow: Flow<Asset>
    ): ParachainStakingRewardsComponent = RealParachainStakingRewardsComponent(
        rewardCalculatorFactory = rewardCalculatorFactory,
        singleAssetSharedState = singleAssetSharedState,
        resourceManager = resourceManager,
        parentScope = parentScope,
        assetFlow = assetFlow
    )
}

private class RealParachainStakingRewardsComponent(
    private val rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val singleAssetSharedState: AnySelectedAssetOptionSharedState,
    private val resourceManager: ResourceManager,
    private val parentScope: CoroutineScope,
    private val assetFlow: Flow<Asset>,
) : ParachainStakingRewardsComponent, CoroutineScope by parentScope {

    private val rewardCalculator by lazyAsync {
        rewardCalculatorFactory.create(singleAssetSharedState.chainAsset())
    }

    override val events = MutableLiveData<Event<Nothing>>()

    private val rewardConfiguration = MutableStateFlow(initialConfiguration())

    override val state: Flow<State?> = rewardConfiguration.flatMapLatest {
        val returns = if (it.collator != null) {
            rewardCalculator().calculateCollatorAnnualReturns(it.collator, it.amount)
        } else {
            rewardCalculator().calculateMaxAnnualReturns(it.amount)
        }

        assetFlow.map { asset ->
            State(
                rewardsConfiguration = it,
                rewardEstimation = mapPeriodReturnsToRewardEstimation(
                    periodReturns = returns,
                    token = asset.token,
                    resourceManager = resourceManager,
                    rewardSuffix = RewardSuffix.APR
                )
            )
        }
    }

    override fun onAction(action: Action) {
        when (action) {
            is Action.ConfigurationUpdated -> {
                rewardConfiguration.value = action.newConfiguration
            }
        }
    }

    private fun initialConfiguration(): RewardsConfiguration {
        return RewardsConfiguration(
            collator = null,
            amount = BigDecimal.ZERO
        )
    }
}
