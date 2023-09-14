package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.setup

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.buildSpannable
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.findById
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.SelectedCollatorSorting
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.SelectedCollator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.accountId
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.estimatedAprReturns
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostConfiguration
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostParameters
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostTask
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.frequencyInDays
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.RewardSuffix
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.format
import io.novafoundation.nova.feature_staking_impl.presentation.mappers.mapPeriodReturnsToRewardEstimation
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorToCollatorParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.collators.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators.mapCollatorToSelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.SelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.common.formatDaysFrequency
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.common.yieldBoostValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfigurationParcel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfirmPayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

sealed class YieldBoostStateModel {

    object Off : YieldBoostStateModel()

    class On(val frequencyTitle: String) : YieldBoostStateModel()
}

private const val FEE_DEBOUNCE_MILLIS = 500L

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
    private val validationSystem: YieldBoostValidationSystem,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin {

    private val validationInProgressFlow = MutableStateFlow(false)

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
        .mapLatest { collatorsUseCase.getSelectedCollators(it, SelectedCollatorSorting.APR) }
        .shareInBackground()

    private val selectedCollatorFlow = MutableSharedFlow<Collator>(replay = 1)
    private val selectedCollatorIdFlow = selectedCollatorFlow.map { it.accountId() }

    val selectedCollatorModel = combine(
        selectedCollatorFlow,
        currentDelegatorStateFlow,
        assetFlow
    ) { selectedCollator, currentDelegatorState, asset ->
        mapCollatorToSelectCollatorModel(selectedCollator, currentDelegatorState, asset, addressIconGenerator, resourceManager)
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
        boostThresholdChooserMixin.amountInput,
        validationInProgressFlow
    ) { activeConfiguration, modifiedConfiguration, amountInput, validationInProgress ->
        when {
            validationInProgress -> DescriptiveButtonState.Loading
            activeConfiguration == modifiedConfiguration -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_no_changes))
            amountInput.isEmpty() -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }
        .onStart { emit(DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_no_changes))) }
        .shareInBackground()

    init {
        setInitialCollator()

        updateYieldBoostStateOnCollatorChange()

        listenFee()
    }

    fun selectCollatorClicked() = launch {
        val delegatorState = currentDelegatorStateFlow.first()
        val alreadyStakedCollators = selectedCollatorsFlow.first()
        val activeTasks = activeTasksFlow.first()

        val payload = createSelectCollatorPayload(alreadyStakedCollators, delegatorState, activeTasks)
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

    @OptIn(FlowPreview::class)
    private fun listenFee() {
        feeLoaderMixin.connectWith(
            inputSource = modifiedYieldBoostConfiguration.debounce(FEE_DEBOUNCE_MILLIS),
            scope = this,
            feeConstructor = { interactor.calculateFee(it, activeTasksFlow.first()) },
        )
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
        return when (modifiedConfiguration) {
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
        val optimalFrequencyFormatted = resourceManager.formatDaysFrequency(optimalFrequency)

        return if (currentFrequency != null && currentFrequency != optimalFrequency) {
            val currentFrequencyFormatted = resourceManager.formatDaysFrequency(currentFrequency)

            resourceManager.getString(R.string.staking_turing_frequency_update_title, optimalFrequencyFormatted, currentFrequencyFormatted)
        } else {
            resourceManager.getString(R.string.staking_turing_frequency_new_title, optimalFrequencyFormatted)
        }
    }

    private suspend fun createSelectCollatorPayload(
        stakedCollators: List<SelectedCollator>,
        delegatorState: DelegatorState,
        activeTasks: List<YieldBoostTask>,
    ): ChooseStakedStakeTargetsBottomSheet.Payload<SelectCollatorModel> {
        val selectedCollator = selectedCollatorFlow.first()
        val chain = delegatorState.chain

        return withContext(Dispatchers.Default) {
            val activeTaskCollatorIds = activeTasks.yieldBoostedCollatorIdsSet()

            val collatorModels = stakedCollators.map {
                SelectCollatorModel(
                    addressModel = addressIconGenerator.collatorAddressModel(it.collator, chain),
                    subtitle = selectCollatorSubsTitle(
                        collator = it.collator,
                        hasActiveYieldBoost = it.collator.accountIdHex in activeTaskCollatorIds
                    ),
                    active = true,
                    payload = it.collator
                )
            }
            val selected = collatorModels.findById(selectedCollator)

            ChooseStakedStakeTargetsBottomSheet.Payload(collatorModels, selected)
        }
    }

    private fun setInitialCollator() = launch(Dispatchers.Default) {
        val alreadyStakedCollators = selectedCollatorsFlow.first()
        val activeTasks = activeTasksFlow.first()
        val yieldBoostedCollatorsSet = activeTasks.yieldBoostedCollatorIdsSet()

        val mostRelevantCollator = alreadyStakedCollators
            .firstOrNull { it.collator.accountIdHex in yieldBoostedCollatorsSet }
            ?: alreadyStakedCollators.first()

        selectedCollatorFlow.emit(mostRelevantCollator.collator)
    }

    private fun maybeGoToNext() = requireFee { fee ->
        launch {
            val payload = YieldBoostValidationPayload(
                collator = selectedCollatorFlow.first(),
                configuration = modifiedYieldBoostConfiguration.first(),
                fee = fee,
                activeTasks = activeTasksFlow.first(),
                asset = assetFlow.first()
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { yieldBoostValidationFailure(it, resourceManager) },
                progressConsumer = validationInProgressFlow.progressConsumer()
            ) {
                validationInProgressFlow.value = false

                goToNextStep(fee = it.fee, collator = it.collator, configuration = it.configuration)
            }
        }
    }

    private fun goToNextStep(
        fee: BigDecimal,
        configuration: YieldBoostConfiguration,
        collator: Collator,
    ) = launch {
        val payload = withContext(Dispatchers.Default) {
            YieldBoostConfirmPayload(
                fee = fee,
                configurationParcel = YieldBoostConfigurationParcel(configuration),
                collator = mapCollatorToCollatorParcelModel(collator)
            )
        }

        router.openConfirmYieldBoost(payload)
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
                frequencyInDays = collatorTask.frequencyInDays() ?: 0, // TODO this creates an invalid state
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

    private fun selectCollatorSubsTitle(collator: Collator, hasActiveYieldBoost: Boolean): CharSequence {
        return buildSpannable(resourceManager) {
            val aprText = RewardSuffix.APR.format(resourceManager, collator.apr.orZero())

            appendColored(aprText, R.color.text_positive)

            if (hasActiveYieldBoost) {
                appendColored(", ", R.color.text_positive)
                appendColored(resourceManager.getString(R.string.yiled_boost_yield_boosted), R.color.text_tertiary)
            }
        }
    }

    private fun List<YieldBoostTask>.yieldBoostedCollatorIdsSet(): Set<String> {
        return mapToSet { it.collator.toHexString() }
    }
}
