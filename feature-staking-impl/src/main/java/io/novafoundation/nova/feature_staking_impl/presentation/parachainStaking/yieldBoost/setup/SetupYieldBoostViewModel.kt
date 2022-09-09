package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.setup

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.castOrNull
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
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostConfiguration
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostParameters
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
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

sealed class YieldBoostStateModel {

    object Off : YieldBoostStateModel()

    class On(val frequencyTitle: String) : YieldBoostStateModel()
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

    private val optimalYieldBoostParameters = combine(currentDelegatorStateFlow, selectedCollatorIdFlow) { delegatorState, collatorId ->
        interactor.optimalYieldBoostParameters(delegatorState, collatorId)
    }.shareInBackground()

    val rewardsWithYieldBoost = optimalYieldBoostParameters.map {
        mapPeriodReturnsToRewardEstimation(
            periodReturns = it.yearlyReturns,
            token = assetFlow.first().token,
            resourceManager = resourceManager,
            rewardSuffix = RewardSuffix.APY
        )
    }.shareInBackground()

    private val activeTasksFlow = currentDelegatorStateFlow.flatMapLatest(interactor::activeYieldBoostTasks)
        .shareInBackground()

    private val activeYieldBoostConfiguration = combine(activeTasksFlow, selectedCollatorFlow, ::constructActiveConfiguration)
        .shareInBackground()

    private val modifiedYieldBoostEnabled = MutableStateFlow(false)

    private val modifiedYieldBoostConfiguration = combine(
        modifiedYieldBoostEnabled,
        boostThresholdChooserMixin.amount,
        optimalYieldBoostParameters,
        selectedCollatorFlow,
        ::constructModifiedConfiguration
    )
        .shareInBackground()

    val configurationUi = combine(activeYieldBoostConfiguration, modifiedYieldBoostConfiguration, ::createYieldBoostUiState)
        .shareInBackground()

    val buttonState = combine(
        activeYieldBoostConfiguration,
        modifiedYieldBoostConfiguration,
        boostThresholdChooserMixin.amountInput
    ) { activeConfiguration, modifiedConfiguration, amountInput ->
        when {
            amountInput.isEmpty() -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
            activeConfiguration == modifiedConfiguration -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_no_changes))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }
        .shareInBackground()

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

    fun yieldBoostStateChanged(yieldBoostOn: Boolean) {
        modifiedYieldBoostEnabled.value = yieldBoostOn
    }

    private fun updateYieldBoostStateOnCollatorChange() {
        activeYieldBoostConfiguration
            .distinctUntilChangedBy { it.collatorIdHex }
            .onEach {
                setActiveAmount(it)
                setIsEnabled(it)
            }
            .inBackground()
            .launchIn(this)
    }

    private fun setIsEnabled(configuration: YieldBoostConfiguration) {
        modifiedYieldBoostEnabled.value = configuration is YieldBoostConfiguration.On
    }

    private suspend fun setActiveAmount(configuration: YieldBoostConfiguration) {
        val newAmount = if (configuration is YieldBoostConfiguration.On) {
            assetFlow.first().token.amountFromPlanks(configuration.threshold).format()
        } else {
            ""
        }

        boostThresholdChooserMixin.amountInput.value = newAmount
    }

    private fun createYieldBoostUiState(
        activeConfiguration: YieldBoostConfiguration,
        modifiedConfiguration: YieldBoostConfiguration
    ): YieldBoostStateModel {
        return when(modifiedConfiguration) {
            is YieldBoostConfiguration.Off -> YieldBoostStateModel.Off
            is YieldBoostConfiguration.On -> {
               val optionalFrequency = modifiedConfiguration.frequencyInDays
                val activeFrequency = activeConfiguration.castOrNull<YieldBoostConfiguration.On>()?.frequencyInDays

                val title = createFrequencyTitle(optionalFrequency, activeFrequency)

                YieldBoostStateModel.On(title)
            }
        }
    }

    private fun createFrequencyTitle(optimalFrequency: Int, currentFrequency: Int?): String {
        val optimalFrequencyFormatted = resourceManager.getQuantityString(R.plurals.common_frequency_days, optimalFrequency, optimalFrequency.format())

        return if (currentFrequency != null && currentFrequency != optimalFrequency) {
            val currentFrequencyFormatted = resourceManager.getQuantityString(R.plurals.common_frequency_days, currentFrequency, currentFrequency.format())

            resourceManager.getString(R.string.staking_turing_frequency_update_title, optimalFrequencyFormatted, currentFrequencyFormatted)
        } else {
            resourceManager.getString(R.string.staking_turing_frequency_new_title, optimalFrequencyFormatted)
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

    private fun constructActiveConfiguration(tasks: List<YieldBoostTask>, collator: Collator): YieldBoostConfiguration {
        val collatorId = collator.accountId()
        val collatorTask = tasks.find { it.collator.contentEquals(collatorId) }

        return if (collatorTask != null) {
            YieldBoostConfiguration.On(
                threshold = collatorTask.accountMinimum,
                frequencyInDays = collatorTask.frequencyInDays(),
                collatorIdHex = collator.accountIdHex
            )
        } else {
            YieldBoostConfiguration.Off(collator.accountIdHex)
        }
    }

    private suspend fun constructModifiedConfiguration(
        enabled: Boolean,
        threshold: BigDecimal,
        optimalParams: YieldBoostParameters,
        collator: Collator,
    ): YieldBoostConfiguration {
        return if (enabled) {
            val thresholdPlanks = assetFlow.first().token.planksFromAmount(threshold)

            YieldBoostConfiguration.On(
                threshold = thresholdPlanks,
                frequencyInDays = optimalParams.periodInDays,
                collatorIdHex = collator.accountIdHex
            )
        } else {
            YieldBoostConfiguration.Off(collator.accountIdHex)
        }
    }
}
