package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.findById
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.ParachainStakingUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.UnbondingCollator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorToCollatorParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators.mapCollatorToSelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators.mapUnbondingCollatorToSelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.SelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.model.ParachainStakingUnbondConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.hints.ParachainStakingUnbondHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.parachainStakingUnbondPayloadAutoFix
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.parachainStakingUnbondValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.transferableAmountModel
import io.novasama.substrate_sdk_android.extensions.fromHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class ParachainStakingUnbondViewModel(
    private val router: ParachainStakingRouter,
    private val interactor: ParachainStakingUnbondInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: ParachainStakingUnbondValidationSystem,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val hintsMixinFactory: ParachainStakingUnbondHintsMixinFactory,
    private val maxActionProviderFactory: MaxActionProviderFactory,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    private val amountFormatter: AmountFormatter
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor {

    private val validationInProgress = MutableStateFlow(false)

    private val assetWithOption = assetUseCase.currentAssetAndOptionFlow()
        .shareInBackground()

    private val selectedAsset = assetWithOption.map { it.asset }
        .shareInBackground()

    private val selectedChainAsset = selectedAsset.map { it.token.configuration }
        .shareInBackground()

    private val currentDelegatorStateFlow = delegatorStateUseCase.currentDelegatorStateFlow()
        .shareInBackground()

    private val alreadyStakedCollatorsFlow = currentDelegatorStateFlow
        .mapLatest(interactor::getSelectedCollators)
        .shareInBackground()

    private val selectedCollatorFlow = singleReplaySharedFlow<Collator>()
    private val selectedCollatorIdFlow = selectedCollatorFlow.map { it.accountIdHex.fromHex() }

    private val stakedAmount = combine(
        currentDelegatorStateFlow,
        selectedCollatorIdFlow
    ) { delegatorState, collatorId ->
        delegatorState.delegationAmountTo(collatorId).orZero()
    }

    val originFeeMixin = feeLoaderMixinFactory.createDefault(
        this,
        selectedChainAsset,
        FeeLoaderMixinV2.Configuration(onRetryCancelled = ::backClicked)
    )

    override val retryEvent: MutableLiveData<Event<RetryPayload>> = originFeeMixin.retryEvent

    private val maxActionProvider = maxActionProviderFactory.createCustom(viewModelScope) {
        selectedChainAsset.providingBalance(stakedAmount)
    }

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = selectedAsset,
        maxActionProvider = maxActionProvider
    )

    val selectedCollatorModel = combine(
        selectedCollatorFlow,
        currentDelegatorStateFlow,
        selectedAsset
    ) { selectedCollator, currentDelegatorState, asset ->
        mapCollatorToSelectCollatorModel(selectedCollator, currentDelegatorState, asset, addressIconGenerator, resourceManager, amountFormatter)
    }.shareInBackground()

    val chooseCollatorAction = actionAwaitableMixinFactory.create<ChooseStakedStakeTargetsBottomSheet.Payload<SelectCollatorModel>, SelectCollatorModel>()

    val minimumStake = selectedCollatorFlow.map {
        val minimumStake = it.minimumStakeToGetRewards
        val asset = selectedAsset.first()

        amountFormatter.formatAmountToAmountModel(minimumStake, asset)
    }.shareInBackground()

    val transferable = selectedAsset.map { it.transferableAmountModel(amountFormatter) }
        .shareInBackground()

    val buttonState = combine(
        validationInProgress,
        amountChooserMixin.amountInput
    ) { validationInProgress, amountInput ->
        when {
            validationInProgress -> DescriptiveButtonState.Loading
            amountInput.isEmpty() -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }

    val hintsMixin = hintsMixinFactory.create(coroutineScope = this)

    init {
        originFeeMixin.connectWith(
            inputSource1 = amountChooserMixin.backPressuredPlanks,
            inputSource2 = selectedCollatorIdFlow,
            feeConstructor = { _, amount, collatorId ->
                interactor.estimateFee(amount, collatorId)
            }
        )

        setInitialCollator()
    }

    fun selectCollatorClicked() = launch {
        val delegatorState = currentDelegatorStateFlow.first()
        val alreadyStakedCollators = alreadyStakedCollatorsFlow.first()

        val payload = createSelectCollatorPayload(alreadyStakedCollators, delegatorState)

        val newCollator = chooseCollatorAction.awaitAction(payload)
        setCollatorIfCanUnbond(newCollator, delegatorState)
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    private suspend fun createSelectCollatorPayload(
        alreadyStakedCollators: List<UnbondingCollator>,
        delegatorState: DelegatorState
    ): ChooseStakedStakeTargetsBottomSheet.Payload<SelectCollatorModel> {
        val asset = selectedAsset.first()
        val selectedCollator = selectedCollatorFlow.first()

        return withContext(Dispatchers.Default) {
            val collatorModels = alreadyStakedCollators.map {
                mapUnbondingCollatorToSelectCollatorModel(
                    unbondingCollator = it,
                    chain = delegatorState.chain,
                    asset = asset,
                    addressIconGenerator = addressIconGenerator,
                    resourceManager = resourceManager,
                    amountFormatter = amountFormatter
                )
            }
            val selected = collatorModels.findById(selectedCollator)

            ChooseStakedStakeTargetsBottomSheet.Payload(collatorModels, selected)
        }
    }

    private suspend fun setCollatorIfCanUnbond(newCollator: SelectCollatorModel, delegatorState: DelegatorState) {
        val collarAccountId = newCollator.payload.accountIdHex.fromHex()

        if (interactor.canUnbond(collarAccountId, delegatorState)) {
            selectedCollatorFlow.emit(newCollator.payload)
        } else {
            showError(
                title = resourceManager.getString(R.string.staking_parachain_unbond_already_exists_title),
                text = resourceManager.getString(R.string.staking_parachain_unbond_already_exists_message)
            )
        }
    }

    private fun setInitialCollator() = launch(Dispatchers.Default) {
        val collatorsWithoutUnbonding = alreadyStakedCollatorsFlow.first()
            .filterNot { it.hasPendingUnbonding }

        if (collatorsWithoutUnbonding.isNotEmpty()) {
            selectedCollatorFlow.emit(collatorsWithoutUnbonding.first().target)
        }
    }

    private fun maybeGoToNext() = launch {
        validationInProgress.value = true

        val payload = ParachainStakingUnbondValidationPayload(
            amount = amountChooserMixin.amount.first(),
            fee = originFeeMixin.awaitFee(),
            asset = selectedAsset.first(),
            collator = selectedCollatorFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { parachainStakingUnbondValidationFailure(it, resourceManager, amountFormatter) },
            autoFixPayload = ::parachainStakingUnbondPayloadAutoFix,
            progressConsumer = validationInProgress.progressConsumer()
        ) { fixedPayload ->
            validationInProgress.value = false

            goToNextStep(fee = fixedPayload.fee, amount = fixedPayload.amount, collator = fixedPayload.collator)
        }
    }

    private fun goToNextStep(
        fee: Fee,
        amount: BigDecimal,
        collator: Collator,
    ) = launch {
        val payload = withContext(Dispatchers.Default) {
            ParachainStakingUnbondConfirmPayload(
                collator = mapCollatorToCollatorParcelModel(collator),
                amount = amount,
                fee = mapFeeToParcel(fee)
            )
        }

        router.openConfirmUnbond(payload)
    }
}
