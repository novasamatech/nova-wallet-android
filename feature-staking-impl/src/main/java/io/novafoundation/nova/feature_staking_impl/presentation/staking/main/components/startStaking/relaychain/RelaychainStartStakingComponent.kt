package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.relaychain

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.rewards.calculateMaxPeriodReturns
import io.novafoundation.nova.feature_staking_impl.domain.rewards.maxCompoundAPY
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StakingRewardEstimationBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.ReturnsModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.StartStakingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.welcomeStakingValidationFailure
import io.novafoundation.nova.runtime.state.SingleAssetSharedState.AssetWithChain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PERIOD_MONTH = 30

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
        assetWithChain: AssetWithChain,
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

    private val assetWithChain: AssetWithChain,
    private val hostContext: ComponentHostContext,
) : StartStakingComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    private val currentSetupProgress = setupStakingSharedState.get<SetupStakingProcess.Initial>()

    private val rewardCalculator = async { rewardCalculatorFactory.create(assetWithChain.chain.id) }

    val selectedAccountStakingStateFlow = hostContext.selectedAccount.flatMapLatest {
        stakingInteractor.selectedAccountStakingStateFlow(it, assetWithChain)
    }.shareInBackground()

    override val events = MutableLiveData<Event<StartStakingEvent>>()

    private val returnsFlow = flowOf {
        val rewardCalculator = rewardCalculator()

        ReturnsModel(
            monthlyPercentage = rewardCalculator.calculateMaxPeriodReturns(PERIOD_MONTH).formatFractionAsPercentage(),
            yearlyPercentage = rewardCalculator.maxCompoundAPY().formatFractionAsPercentage()
        )
    }
        .withLoading()

    private val _state = returnsFlow.mapLatest { returnsState ->
        StartStakingState(
            estimateEarningsTitle = estimateEarningsTitle(),
            returns = returnsState
        )
    }

    override val state: Flow<StartStakingState?> = selectedAccountStakingStateFlow.transform { stakingState ->
        if (stakingState !is StakingState.NonStash) {
            emit(null)
        } else {
            emitAll(_state)
        }
    }
        .onStart { emit(null) }
        .shareInBackground()

    override fun onAction(action: StartStakingAction) {
        launch {
            when (action) {
                StartStakingAction.InfoClicked -> infoClicked()
                StartStakingAction.NextClicked -> nextClicked()
            }
        }
    }

    private fun estimateEarningsTitle(): String {
        val assetSymbol = assetWithChain.asset.symbol

        return resourceManager.getString(R.string.staking_estimate_earning_title_v2_2_0, assetSymbol)
    }

    private suspend fun infoClicked() = withContext(Dispatchers.Default) {
        val rewardCalculator = rewardCalculator()

        val payload = StakingRewardEstimationBottomSheet.Payload(
            max = rewardCalculator.maxCompoundAPY().formatFractionAsPercentage(),
            average = rewardCalculator.expectedAPY.formatFractionAsPercentage()
        )
        val showRewardEstimationDetails = StartStakingEvent.ShowRewardEstimationDetails(payload)

        events.postValue(showRewardEstimationDetails.event())
    }

    private suspend fun nextClicked() {
        val payload = WelcomeStakingValidationPayload()

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            errorDisplayer = hostContext.errorDisplayer,
            validationFailureTransformerDefault = { welcomeStakingValidationFailure(it, resourceManager) },
        ) {
            setupStakingSharedState.set(currentSetupProgress.fullFlow())

            router.openSetupStaking()
        }
    }
}
