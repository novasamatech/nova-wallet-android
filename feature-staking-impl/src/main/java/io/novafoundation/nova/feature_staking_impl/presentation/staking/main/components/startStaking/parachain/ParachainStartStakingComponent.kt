package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.parachain

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.maximumAnnualApr
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StakingRewardEstimationBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.BaseStartStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingEvent
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ParachainStartStakingComponentFactory(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
) {

    fun create(
        assetWithChain: SingleAssetSharedState.AssetWithChain,
        hostContext: ComponentHostContext
    ): StartStakingComponent = ParachainStartStakingComponent(
        delegatorStateUseCase = delegatorStateUseCase,
        rewardCalculatorFactory = rewardCalculatorFactory,
        resourceManager = resourceManager,
        router = router,
        assetWithChain = assetWithChain,
        hostContext = hostContext
    )
}

private class ParachainStartStakingComponent(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,

    private val assetWithChain: SingleAssetSharedState.AssetWithChain,
    hostContext: ComponentHostContext,
) : BaseStartStakingComponent(assetWithChain, hostContext, resourceManager) {

    private val rewardCalculator = async { rewardCalculatorFactory.create(assetWithChain.chain.id) }

    private val delegatorStateFlow = hostContext.selectedAccount.flatMapLatest {
        delegatorStateUseCase.delegatorStateFlow(it, assetWithChain.chain, assetWithChain.asset)
    }.shareInBackground()

    override suspend fun maxPeriodReturnPercentage(days: Int): BigDecimal {
        return rewardCalculator().maximumApr(days)
    }

    override val isComponentApplicable = delegatorStateFlow.map { it is DelegatorState.None }

    override suspend fun infoClicked() = withContext(Dispatchers.Default) {
        val rewardCalculator = rewardCalculator()

        val payload = StakingRewardEstimationBottomSheet.Payload(
            max = rewardCalculator.maximumAnnualApr().formatFractionAsPercentage(),
            average = rewardCalculator.averageAnnualApr().formatFractionAsPercentage(),
            returnsTypeFormat = R.string.staking_apr,
            title = resourceManager.getString(R.string.staking_reward_info_title_transferrable)
        )
        val showRewardEstimationDetails = StartStakingEvent.ShowRewardEstimationDetails(payload)

        events.postValue(showRewardEstimationDetails.event())
    }

    override suspend fun nextClicked() {
        // TODO
    }
}
