package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.relaychain

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.formatting.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.rewards.calculateMaxPeriodReturns
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StakingRewardEstimationBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.BaseStartStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.welcomeStakingValidationFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class RelaychainStartStakingComponentFactory(
    private val setupStakingSharedState: SetupStakingSharedState,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val validationSystem: WelcomeStakingValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val stakingSharedComputation: StakingSharedComputation,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): StartStakingComponent = RelaychainStartStakingComponent(
        stakingSharedComputation = stakingSharedComputation,
        setupStakingSharedState = setupStakingSharedState,
        resourceManager = resourceManager,
        router = router,
        validationSystem = validationSystem,
        validationExecutor = validationExecutor,
        stakingOption = stakingOption,
        hostContext = hostContext
    )
}

private class RelaychainStartStakingComponent(
    private val stakingSharedComputation: StakingSharedComputation,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val validationSystem: WelcomeStakingValidationSystem,
    private val validationExecutor: ValidationExecutor,

    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
) : BaseStartStakingComponent(stakingOption, hostContext, resourceManager) {

    private val currentSetupProgress = setupStakingSharedState.get<SetupStakingProcess.Initial>()

    private val rewardCalculator = async { stakingSharedComputation.rewardCalculator(stakingOption, hostContext.scope) }

    private val selectedAccountStakingStateFlow = stakingSharedComputation.selectedAccountStakingStateFlow(
        assetWithChain = stakingOption.assetWithChain,
        scope = hostContext.scope
    )

    override suspend fun maxPeriodReturnPercentage(days: Int): BigDecimal {
        return rewardCalculator().calculateMaxPeriodReturns(days)
    }

    override val isComponentApplicable = selectedAccountStakingStateFlow.map { it is StakingState.NonStash }

    override suspend fun infoClicked() = withContext(Dispatchers.Default) {
        val rewardCalculator = rewardCalculator()

        val payload = StakingRewardEstimationBottomSheet.Payload(
            max = rewardCalculator.maxAPY.toBigDecimal().formatFractionAsPercentage(),
            average = rewardCalculator.expectedAPY.formatFractionAsPercentage(),
            returnsTypeFormat = R.string.staking_apy,
            title = resourceManager.getString(R.string.staking_reward_info_title_restake)
        )
        val showRewardEstimationDetails = StartStakingEvent.ShowRewardEstimationDetails(payload)

        events.postValue(showRewardEstimationDetails.event())
    }

    override suspend fun nextClicked() {
        val payload = WelcomeStakingValidationPayload(
            chain = stakingOption.assetWithChain.chain,
            metaAccount = hostContext.selectedAccount.first(),
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            errorDisplayer = hostContext.errorDisplayer,
            validationFailureTransformerCustom = { status, _ -> welcomeStakingValidationFailure(status.reason, resourceManager, router) },
        ) {
            setupStakingSharedState.set(currentSetupProgress.fullFlow())

            router.openSetupStaking()
        }
    }
}
