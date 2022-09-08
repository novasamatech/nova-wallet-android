package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.setup

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.findById
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.SelectedCollator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.accountId
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.estimatedAprReturns
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostTask
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.frequencyInDays
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.RewardSuffix
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapPeriodReturnsToRewardEstimation
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorToCollatorParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators.mapCollatorToSelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators.mapSelectedCollatorToSelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.SelectCollatorModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

sealed class YieldBoostState {

    object Off : YieldBoostState()

    class On(val frequencyTitle: String): YieldBoostState()
}

class SetupYieldBoostViewModel(
    private val router: ParachainStakingRouter,
    private val interactor: YieldBoostInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val collatorsUseCase: CollatorsUseCase,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin {

    private val validationInProgress = MutableStateFlow(false)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .share()

    val boostThresholdChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        balanceField = Asset::transferable,
        balanceLabel = R.string.wallet_balance_transferable
    )

    val chooseCollatorAction = actionAwaitableMixinFactory.create<ChooseStakedStakeTargetsBottomSheet.Payload<SelectCollatorModel>, SelectCollatorModel>()

    private val currentDelegatorStateFlow = delegatorStateUseCase.currentDelegatorStateFlow()
        .filterIsInstance<DelegatorState.Delegator>()
        .shareInBackground()

    private val selectedCollatorsFlow = currentDelegatorStateFlow
        .mapLatest(collatorsUseCase::getSelectedCollators)
        .shareInBackground()

    private val selectedCollatorFlow = MutableSharedFlow<Collator>(replay = 1)
    private val selectedCollatorIdFlow = selectedCollatorFlow.map { it.accountId() }

    val selectedCollatorModel = combine(
        selectedCollatorFlow,
        currentDelegatorStateFlow,
        assetFlow
    ) { selectedCollator, currentDelegatorState, asset ->
        mapCollatorToSelectCollatorModel(selectedCollator, currentDelegatorState, asset, addressIconGenerator)
    }.shareInBackground()

    val rewardsWithoutYieldBoost = combine(currentDelegatorStateFlow, selectedCollatorFlow) { delegatorState, collator ->
        val token = assetFlow.first().token
        val delegationPlanks = delegatorState.delegationAmountTo(collator.accountId()).orZero()
        val periodReturns = collator.estimatedAprReturns(token.amountFromPlanks(delegationPlanks))

        mapPeriodReturnsToRewardEstimation(
            periodReturns = periodReturns,
            token = assetFlow.first().token,
            resourceManager = resourceManager,
            rewardSuffix = RewardSuffix.APR
        )
    }.shareInBackground()

    private val yieldBoostParameters = combine(currentDelegatorStateFlow, selectedCollatorIdFlow) { delegatorState, collatorId ->
        interactor.optimalYieldBoostParameters(delegatorState, collatorId)
    }.shareInBackground()

    val rewardsWithYieldBoost = yieldBoostParameters.map {
        mapPeriodReturnsToRewardEstimation(
            periodReturns = it.yearlyReturns,
            token = assetFlow.first().token,
            resourceManager = resourceManager,
            rewardSuffix = RewardSuffix.APY
        )
    }.shareInBackground()

    private val activeTasksFlow = currentDelegatorStateFlow.flatMapLatest(interactor::activeYieldBoostTasks)
        .shareInBackground()

    val yieldBoostState = MutableStateFlow<YieldBoostState>(YieldBoostState.Off)

    init {
        setInitialCollator()

        updateYieldBoostStateOnCollatorChange()
    }

    fun selectCollatorClicked() = launch {
        val delegatorState = currentDelegatorStateFlow.first()
        val alreadyStakedCollators = selectedCollatorsFlow.first()

        val payload = createSelectCollatorPayload(alreadyStakedCollators, delegatorState)
        val newSelectedCollatorModel = chooseCollatorAction.awaitAction(payload)

        selectedCollatorFlow.emit(newSelectedCollatorModel.payload)
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun yieldBoostStateChanged(yieldBoostOn: Boolean)  = launch(Dispatchers.Default) {
        setNewYieldBoostState(haveYieldBoost = yieldBoostOn)
    }

    private fun updateYieldBoostStateOnCollatorChange() {
        selectedCollatorFlow.map {
            setNewYieldBoostState(haveYieldBoost = selectedCollatorTask() != null)
        }
            .inBackground()
            .launchIn(this)
    }

    private suspend fun setNewYieldBoostState(haveYieldBoost: Boolean) {
        if (haveYieldBoost) {
            val selectedCollatorTask = selectedCollatorTask()
            val optimalFrequency = yieldBoostParameters.first().periodInDays

            if (selectedCollatorTask != null) {
                val currentFrequency = selectedCollatorTask.frequencyInDays()
                yieldBoostState.value = YieldBoostState.On(createFrequencyTitle(optimalFrequency, currentFrequency))

                val currentMinimum = selectedCollatorTask.accountMinimum
                val currentMinimumInput = assetFlow.first().token.amountFromPlanks(currentMinimum).format()
                boostThresholdChooserMixin.amountInput.value = currentMinimumInput
            } else {
                yieldBoostState.value = YieldBoostState.On(createFrequencyTitle(optimalFrequency, currentFrequency = null))

                boostThresholdChooserMixin.amountInput.value = ""
            }
        } else {
            yieldBoostState.value = YieldBoostState.Off
        }
    }

    private fun createFrequencyTitle(optimalFrequency: Int, currentFrequency: Int?): String {
        val optimalFrequencyFormatted = resourceManager.getQuantityString(R.plurals.common_frequency_days, optimalFrequency, optimalFrequency.format())

        return if (currentFrequency != null && currentFrequency != optimalFrequency) {
            val currentFrequencyFormatted = resourceManager.getQuantityString(R.plurals.common_frequency_days, currentFrequency, currentFrequency.format())

            resourceManager.getString(R.string.staking_turing_frequency_update_title,  optimalFrequencyFormatted, currentFrequencyFormatted)
        } else {
            resourceManager.getString(R.string.staking_turing_frequency_new_title,  optimalFrequencyFormatted)
        }
    }

    private suspend fun createSelectCollatorPayload(
        stakedCollators: List<SelectedCollator>,
        delegatorState: DelegatorState
    ): ChooseStakedStakeTargetsBottomSheet.Payload<SelectCollatorModel> {
        val asset = assetFlow.first()
        val selectedCollator = selectedCollatorFlow.first()

        return withContext(Dispatchers.Default) {
            val collatorModels = stakedCollators.map {
                mapSelectedCollatorToSelectCollatorModel(
                    selectedCollator = it,
                    chain = delegatorState.chain,
                    asset = asset,
                    addressIconGenerator = addressIconGenerator
                )
            }
            val selected = collatorModels.findById(selectedCollator)

            ChooseStakedStakeTargetsBottomSheet.Payload(collatorModels, selected)
        }
    }

    private fun setInitialCollator() = launch {
        val alreadyStakedCollators = selectedCollatorsFlow.first()

        if (alreadyStakedCollators.isNotEmpty()) {
            selectedCollatorFlow.emit(alreadyStakedCollators.first().collator)
        }
    }

    private fun maybeGoToNext() = requireFee { fee ->
//        launch {
//            val collator = selectedCollatorFlow.first()
//            val amount = amountChooserMixin.amount.first()
//
//            val payload = StartParachainStakingValidationPayload(
//                amount = amount,
//                fee = fee,
//                asset = assetFlow.first(),
//                collator = collator
//            )
//
//            validationExecutor.requireValid(
//                validationSystem = validationSystem,
//                payload = payload,
//                validationFailureTransformer = { startParachainStakingValidationFailure(it, resourceManager) },
//                progressConsumer = validationInProgress.progressConsumer()
//            ) {
//                validationInProgress.value = false
//
//                goToNextStep(fee = fee, amount = amount, collator = collator)
//            }
//        }
        showMessage("TODO")
    }

    private fun goToNextStep(
        fee: BigDecimal,
        amount: BigDecimal,
        collator: Collator,
    ) = launch {
        val payload = withContext(Dispatchers.Default) {
            ConfirmStartParachainStakingPayload(
                collator = mapCollatorToCollatorParcelModel(collator),
                amount = amount,
                fee = fee
            )
        }
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )

    private suspend fun selectedCollatorTask(): YieldBoostTask? {
        val tasks = activeTasksFlow.first()
        val collator = selectedCollatorFlow.first()

        return tasks.find { it.collator.contentEquals(collator.accountId()) }
    }
}
