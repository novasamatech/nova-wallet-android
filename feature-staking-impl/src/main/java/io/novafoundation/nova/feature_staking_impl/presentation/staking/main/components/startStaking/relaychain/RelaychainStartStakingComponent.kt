package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.relaychain

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.formatting.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
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
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class RelaychainStartStakingComponentFactory(
    private val stakingInteractor: StakingInteractor,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val rewardCalculatorFactory: RewardCalculatorFactory,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val validationSystem: WelcomeStakingValidationSystem,
    private val validationExecutor: ValidationExecutor,
) {

    fun create(
        assetWithChain: ChainWithAsset,
        hostContext: ComponentHostContext
    ): StartStakingComponent = RelaychainStartStakingComponent(
        stakingInteractor = stakingInteractor,
        setupStakingSharedState = setupStakingSharedState,
        rewardCalculatorFactory = rewardCalculatorFactory,
        resourceManager = resourceManager,
        router = router,
        validationSystem = validationSystem,
        validationExecutor = validationExecutor,
        assetWithChain = assetWithChain,
        hostContext = hostContext
    )
}

private class RelaychainStartStakingComponent(
    private val stakingInteractor: StakingInteractor,
    private val setupStakingSharedState: SetupStakingSharedState,
    private val rewardCalculatorFactory: RewardCalculatorFactory,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val validationSystem: WelcomeStakingValidationSystem,
    private val validationExecutor: ValidationExecutor,

    private val assetWithChain: ChainWithAsset,
    private val hostContext: ComponentHostContext,
) : BaseStartStakingComponent(assetWithChain, hostContext, resourceManager) {

    private val currentSetupProgress = setupStakingSharedState.get<SetupStakingProcess.Initial>()

    private val rewardCalculator = async { rewardCalculatorFactory.create(assetWithChain.asset) }

    val selectedAccountStakingStateFlow = hostContext.selectedAccount.flatMapLatest {
        stakingInteractor.selectedAccountStakingStateFlow(it, assetWithChain)
    }.shareInBackground()

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
            chain = assetWithChain.chain,
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
