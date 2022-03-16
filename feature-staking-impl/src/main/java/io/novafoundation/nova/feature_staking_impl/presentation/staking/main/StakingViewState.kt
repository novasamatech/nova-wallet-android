package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatFractionAsPercentage
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.main.ManageStakeAction
import io.novafoundation.nova.feature_staking_impl.domain.model.NominatorStatus
import io.novafoundation.nova.feature_staking_impl.domain.model.NominatorStatus.Inactive.Reason
import io.novafoundation.nova.feature_staking_impl.domain.model.StakeSummary
import io.novafoundation.nova.feature_staking_impl.domain.model.StashNoneStatus
import io.novafoundation.nova.feature_staking_impl.domain.model.ValidatorStatus
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculator
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.rewards.calculateMaxPeriodReturns
import io.novafoundation.nova.feature_staking_impl.domain.rewards.maxCompoundAPY
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.welcome.WelcomeStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingProcess
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.unbonding.UnbondingMixinFactory
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed class StakingViewState(
    coroutineScope: CoroutineScope,
    validationExecutor: ValidationExecutor
) :
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope),
    Validatable by validationExecutor

private const val PERIOD_MONTH = 30

class ReturnsModel(
    val monthlyPercentage: String,
    val yearlyPercentage: String,
)

class StakeSummaryModel<S>(
    val status: S,
    val totalStaked: AmountModel,
)

typealias NominatorSummaryModel = StakeSummaryModel<NominatorStatus>
typealias ValidatorSummaryModel = StakeSummaryModel<ValidatorStatus>
typealias StashNoneSummaryModel = StakeSummaryModel<StashNoneStatus>

@Suppress("LeakingThis")
sealed class StakeViewState<S>(
    private val stakeState: StakingState.Stash,
    protected val currentAssetFlow: Flow<Asset>,
    protected val stakingInteractor: StakingInteractor,
    protected val resourceManager: ResourceManager,
    protected val scope: CoroutineScope,
    protected val router: StakingRouter,
    protected val errorDisplayer: (Throwable) -> Unit,
    protected val summaryFlowProvider: suspend (StakingState.Stash) -> Flow<StakeSummary<S>>,
    protected val statusMessageProvider: (S) -> TitleAndMessage,
    private val validationExecutor: ValidationExecutor,
    private val availableManageActions: Set<ManageStakeAction>,
    private val stakeActionsValidations: Map<ManageStakeAction, StakeActionsValidationSystem>,
    unbondingMixinFactory: UnbondingMixinFactory,
) : StakingViewState(scope, validationExecutor) {

    val unbondingMixin = unbondingMixinFactory.create(
        errorDisplayer = errorDisplayer,
        stashState = stakeState,
        assetFlow = currentAssetFlow,
        coroutineScope = coroutineScope
    )

    init {
        syncStakingRewards()
    }

    val manageStakingActionsButtonVisible = availableManageActions.isNotEmpty()

    private val _showManageActionsEvent = MutableLiveData<Event<ManageStakingBottomSheet.Payload>>()
    val showManageActionsEvent: LiveData<Event<ManageStakingBottomSheet.Payload>> = _showManageActionsEvent

    fun manageActionChosen(action: ManageStakeAction) {
        if (action !in availableManageActions) return

        val validationSystem = stakeActionsValidations[action]

        if (validationSystem != null) {
            val payload = StakeActionsValidationPayload(stakeState)

            scope.launch {
                validationExecutor.requireValid(
                    validationSystem = validationSystem,
                    payload = payload,
                    errorDisplayer = errorDisplayer,
                    validationFailureTransformerDefault = { mainStakingValidationFailure(it, resourceManager) },
                ) {
                    navigateToAction(action)
                }
            }
        } else {
            navigateToAction(action)
        }
    }

    fun moreActionsClicked() {
        _showManageActionsEvent.value = Event(ManageStakingBottomSheet.Payload(availableManageActions))
    }

    val userRewardsFlow = combine(
        stakingInteractor.observeUserRewards(stakeState),
        currentAssetFlow
    ) { totalRewards, currentAsset ->
        mapAmountToAmountModel(totalRewards, currentAsset)
    }
        .withLoading()
        .inBackground()
        .share()

    val stakeSummaryFlow = flow { emitAll(summaryFlow()) }
        .withLoading()
        .inBackground()
        .share()

    private val _showStatusAlertEvent = MutableLiveData<Event<Pair<String, String>>>()
    val showStatusAlertEvent: LiveData<Event<Pair<String, String>>> = _showStatusAlertEvent

    fun statusClicked() {
        val nominatorSummaryModel = loadedSummaryOrNull() ?: return

        val titleAndMessage = statusMessageProvider(nominatorSummaryModel.status)

        _showStatusAlertEvent.value = Event(titleAndMessage)
    }

    private fun navigateToAction(action: ManageStakeAction) {
        when (action) {
            ManageStakeAction.PAYOUTS -> router.openPayouts()
            ManageStakeAction.BALANCE -> router.openStakingBalance()
            ManageStakeAction.CONTROLLER -> router.openControllerAccount()
            ManageStakeAction.VALIDATORS -> router.openCurrentValidators()
            ManageStakeAction.REWARD_DESTINATION -> router.openChangeRewardDestination()
        }
    }

    private fun syncStakingRewards() {
        scope.launch {
            stakingInteractor.syncStakingRewards(stakeState)
        }
    }

    private suspend fun summaryFlow(): Flow<StakeSummaryModel<S>> {
        return combine(
            summaryFlowProvider(stakeState),
            currentAssetFlow
        ) { summary, asset ->
            StakeSummaryModel(
                status = summary.status,
                totalStaked = mapAmountToAmountModel(summary.totalStaked, asset),
            )
        }
    }

    private fun loadedSummaryOrNull(): StakeSummaryModel<S>? {
        return when (val state = stakeSummaryFlow.replayCache.firstOrNull()) {
            is LoadingState.Loaded<StakeSummaryModel<S>> -> state.data
            else -> null
        }
    }
}

class ValidatorViewState(
    validatorState: StakingState.Stash.Validator,
    currentAssetFlow: Flow<Asset>,
    stakingInteractor: StakingInteractor,
    resourceManager: ResourceManager,
    scope: CoroutineScope,
    router: StakingRouter,
    errorDisplayer: (Throwable) -> Unit,
    stakeActionsValidations: Map<ManageStakeAction, StakeActionsValidationSystem>,
    validationExecutor: ValidationExecutor,
    unbondingMixinFactory: UnbondingMixinFactory
) : StakeViewState<ValidatorStatus>(
    validatorState, currentAssetFlow, stakingInteractor,
    resourceManager, scope, router, errorDisplayer,
    summaryFlowProvider = { stakingInteractor.observeValidatorSummary(validatorState) },
    statusMessageProvider = { getValidatorStatusTitleAndMessage(resourceManager, it) },
    availableManageActions = ManageStakeAction.values().toSet() - ManageStakeAction.VALIDATORS,
    validationExecutor = validationExecutor,
    stakeActionsValidations = stakeActionsValidations,
    unbondingMixinFactory = unbondingMixinFactory
)

private fun getValidatorStatusTitleAndMessage(
    resourceManager: ResourceManager,
    status: ValidatorStatus
): Pair<String, String> {
    val (titleRes, messageRes) = when (status) {
        ValidatorStatus.ACTIVE -> R.string.staking_nominator_status_alert_active_title to R.string.staking_nominator_status_alert_active_message

        ValidatorStatus.INACTIVE -> R.string.staking_nominator_status_alert_inactive_title to R.string.staking_nominator_status_alert_no_validators
    }

    return resourceManager.getString(titleRes) to resourceManager.getString(messageRes)
}

class StashNoneViewState(
    stashState: StakingState.Stash.None,
    currentAssetFlow: Flow<Asset>,
    stakingInteractor: StakingInteractor,
    resourceManager: ResourceManager,
    scope: CoroutineScope,
    router: StakingRouter,
    errorDisplayer: (Throwable) -> Unit,
    validationExecutor: ValidationExecutor,
    stakeActionsValidations: Map<ManageStakeAction, StakeActionsValidationSystem>,
    unbondingMixinFactory: UnbondingMixinFactory
) : StakeViewState<StashNoneStatus>(
    stashState, currentAssetFlow, stakingInteractor,
    resourceManager, scope, router, errorDisplayer,
    summaryFlowProvider = { stakingInteractor.observeStashSummary(stashState) },
    statusMessageProvider = { getStashStatusTitleAndMessage(resourceManager, it) },
    availableManageActions = ManageStakeAction.values().toSet() - ManageStakeAction.PAYOUTS,
    validationExecutor = validationExecutor,
    stakeActionsValidations = stakeActionsValidations,
    unbondingMixinFactory = unbondingMixinFactory
)

private fun getStashStatusTitleAndMessage(
    resourceManager: ResourceManager,
    status: StashNoneStatus
): Pair<String, String> {
    val (titleRes, messageRes) = when (status) {
        StashNoneStatus.INACTIVE -> R.string.staking_nominator_status_alert_inactive_title to R.string.staking_bonded_inactive
    }

    return resourceManager.getString(titleRes) to resourceManager.getString(messageRes)
}

class NominatorViewState(
    nominatorState: StakingState.Stash.Nominator,
    currentAssetFlow: Flow<Asset>,
    stakingInteractor: StakingInteractor,
    resourceManager: ResourceManager,
    scope: CoroutineScope,
    router: StakingRouter,
    errorDisplayer: (Throwable) -> Unit,
    validationExecutor: ValidationExecutor,
    stakeActionsValidations: Map<ManageStakeAction, StakeActionsValidationSystem>,
    unbondingMixinFactory: UnbondingMixinFactory
) : StakeViewState<NominatorStatus>(
    nominatorState, currentAssetFlow, stakingInteractor,
    resourceManager, scope, router, errorDisplayer,
    summaryFlowProvider = { stakingInteractor.observeNominatorSummary(nominatorState) },
    statusMessageProvider = { getNominatorStatusTitleAndMessage(resourceManager, it) },
    availableManageActions = ManageStakeAction.values().toSet(),
    validationExecutor = validationExecutor,
    stakeActionsValidations = stakeActionsValidations,
    unbondingMixinFactory = unbondingMixinFactory
)

private fun getNominatorStatusTitleAndMessage(
    resourceManager: ResourceManager,
    status: NominatorStatus
): Pair<String, String> {
    val (titleRes, messageRes) = when (status) {
        is NominatorStatus.Active -> R.string.staking_nominator_status_alert_active_title to R.string.staking_nominator_status_alert_active_message

        is NominatorStatus.Waiting -> R.string.staking_nominator_status_waiting to R.string.staking_nominator_status_alert_waiting_message

        is NominatorStatus.Inactive -> when (status.reason) {
            Reason.MIN_STAKE -> R.string.staking_nominator_status_alert_inactive_title to R.string.staking_nominator_status_alert_low_stake
            Reason.NO_ACTIVE_VALIDATOR -> R.string.staking_nominator_status_alert_inactive_title to R.string.staking_nominator_status_alert_no_validators
        }
    }

    return resourceManager.getString(titleRes) to resourceManager.getString(messageRes)
}

class WelcomeViewState(
    private val setupStakingSharedState: SetupStakingSharedState,
    private val rewardCalculatorFactory: RewardCalculatorFactory,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val scope: CoroutineScope,
    private val errorDisplayer: (String) -> Unit,
    private val validationSystem: WelcomeStakingValidationSystem,
    private val validationExecutor: ValidationExecutor,
    currentAssetFlow: Flow<Asset>,
) : StakingViewState(scope, validationExecutor) {

    private val currentSetupProgress = setupStakingSharedState.get<SetupStakingProcess.Initial>()

    private val rewardCalculator = scope.async { rewardCalculatorFactory.create() }

    private val _showRewardEstimationEvent = MutableLiveData<Event<StakingRewardEstimationBottomSheet.Payload>>()
    val showRewardEstimationEvent: LiveData<Event<StakingRewardEstimationBottomSheet.Payload>> = _showRewardEstimationEvent

    val estimateEarningsTitle = currentAssetFlow.map {
        resourceManager.getString(R.string.staking_estimate_earning_title_v2_2_0, it.token.configuration.symbol)
    }
        .inBackground()
        .share()

    val returns = flowOf {
        val rewardCalculator = rewardCalculator()

        ReturnsModel(
            monthlyPercentage = rewardCalculator.calculateMaxPeriodReturns(PERIOD_MONTH).formatFractionAsPercentage(),
            yearlyPercentage = rewardCalculator.maxCompoundAPY().formatFractionAsPercentage()
        )
    }
        .withLoading()
        .inBackground()
        .share()

    fun infoActionClicked() {
        scope.launch {
            val rewardCalculator = rewardCalculator()

            val payload = StakingRewardEstimationBottomSheet.Payload(
                max = rewardCalculator.maxCompoundAPY().formatFractionAsPercentage(),
                average = rewardCalculator.expectedAPY.formatFractionAsPercentage()
            )

            _showRewardEstimationEvent.value = Event(payload)
        }
    }

    fun nextClicked() {
        scope.launch {
            val payload = WelcomeStakingValidationPayload()

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                errorDisplayer = { it.message?.let(errorDisplayer) },
                validationFailureTransformerDefault = { welcomeStakingValidationFailure(it, resourceManager) },
            ) {
                setupStakingSharedState.set(currentSetupProgress.fullFlow())

                router.openSetupStaking()
            }
        }
    }

    private suspend fun rewardCalculator(): RewardCalculator {
        return rewardCalculator.await()
    }
}
