package io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.setup

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.findById
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.model.TargetWithStakedAmount
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.UnbondMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations.UnbondMythosStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations.UnbondMythosValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.MythosCollatorFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.model.MythosCollatorWithAmount
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.model.MythosSelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model.toParcel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.confirm.ConfirmUnbondMythosPayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setBlockedAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setInputBlocked
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.toParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.model.transferableAmountModel
import io.novafoundation.nova.runtime.state.chainAsset
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet.Payload as SelectCollatorPayload

class SetupUnbondMythosViewModel(
    private val router: MythosStakingRouter,
    private val interactor: UnbondMythosStakingInteractor,
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: UnbondMythosValidationSystem,
    private val feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    private val delegatorStateUseCase: MythosDelegatorStateUseCase,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val mythosSharedComputation: MythosSharedComputation,
    private val mythosCollatorFormatter: MythosCollatorFormatter,
    private val mythosValidationFailureFormatter: MythosStakingValidationFailureFormatter,
    private val stakingSharedState: StakingSharedState,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Validatable by validationExecutor {

    private val validationInProgress = MutableStateFlow(false)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .share()

    private val chainAssetFlow =  stakingSharedState.selectedAssetFlow()

    private val currentDelegatorStateFlow = mythosSharedComputation.delegatorStateFlow()
        .shareInBackground()

    private val alreadyStakedCollatorsFlow = currentDelegatorStateFlow
        .mapLatest { delegatorStateUseCase.getStakedCollators(it) }
        .shareInBackground()

    private val selectedCollatorFlow = singleReplaySharedFlow<MythosCollator>()
    private val selectedCollatorIdFlow = selectedCollatorFlow.map { it.accountId }

    private val selectedCollatorWithStake = combine(selectedCollatorFlow, currentDelegatorStateFlow) { selectedCollator, currentDelegatorState ->
        val stake = currentDelegatorState.delegationAmountTo(selectedCollator.accountId)

        TargetWithStakedAmount(stake, selectedCollator)
    }

    private val stakedAmount = selectedCollatorWithStake.map { it.stake }

    val selectedCollatorModel = combine(
        selectedCollatorWithStake,
        assetFlow
    ) { selectedCollator, asset ->
        mythosCollatorFormatter.collatorToSelectUi(selectedCollator, asset.token)
    }.shareInBackground()

    val chooseCollatorAction = actionAwaitableMixinFactory.create<SelectCollatorPayload<MythosSelectCollatorModel>, MythosSelectCollatorModel>()

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        maxActionProvider = MaxActionProvider.create(viewModelScope) {
            chainAssetFlow.providingBalance(stakedAmount)
        }
    )

    val transferable = assetFlow.map(Asset::transferableAmountModel)
        .shareInBackground()

    val feeLoaderMixin = feeLoaderMixinV2Factory.createDefault(viewModelScope, chainAssetFlow)

    val buttonState = combine(
        validationInProgress,
        amountChooserMixin.inputState
    ) { validationInProgress, inputState ->
        when {
            validationInProgress -> DescriptiveButtonState.Loading
            inputState.value.isEmpty() -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }

    init {
        feeLoaderMixin.connectWith(
            inputSource1 = selectedCollatorIdFlow,
            feeConstructor = { _, collatorId -> interactor.estimateFee(currentDelegatorStateFlow.first(), collatorId) },
        )

        setInitialCollator()

        setupAmountChanges()
    }

    private fun setupAmountChanges() {
        // Block input immediately
        amountChooserMixin.setInputBlocked()

        stakedAmount.onEach { planks ->
            val chainAsset = stakingSharedState.chainAsset()
            val amount = chainAsset.amountFromPlanks(planks)

            amountChooserMixin.setBlockedAmount(amount)
        }.launchIn(this)
    }

    fun selectCollatorClicked() = launch {
        val alreadyStakedCollators = alreadyStakedCollatorsFlow.first()

        val selectedCollator = selectedCollatorFlow.first()
        val asset = assetFlow.first()

        val payload = createSelectCollatorPayload(alreadyStakedCollators, asset, selectedCollator)

        val newCollator = chooseCollatorAction.awaitAction(payload)
        selectedCollatorFlow.emit(newCollator.payload)
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    private suspend fun createSelectCollatorPayload(
        alreadyStakedCollators: List<MythosCollatorWithAmount>,
        asset: Asset,
        selectedCollator: MythosCollator,
    ): SelectCollatorPayload<MythosSelectCollatorModel> {
        val collatorModels = alreadyStakedCollators.map { target ->
            mythosCollatorFormatter.collatorToSelectUi(target, asset.token)
        }
        val selected = collatorModels.findById(selectedCollator)

        return SelectCollatorPayload(collatorModels, selected)
    }

    private fun setInitialCollator() = launch {
        val stakedCollators = alreadyStakedCollatorsFlow.first()

        if (stakedCollators.isNotEmpty()) {
            selectedCollatorFlow.emit(stakedCollators.first().target)
        }
    }

    private fun maybeGoToNext() = launch {
        validationInProgress.value = true

        val payload = UnbondMythosStakingValidationPayload(
            fee = feeLoaderMixin.awaitFee(),
            asset = assetFlow.first(),
            delegatorState = currentDelegatorStateFlow.first(),
            collator = selectedCollatorFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { failure, _ -> mythosValidationFailureFormatter.formatUnbond(failure) },
            progressConsumer = validationInProgress.progressConsumer()
        ) { fixedPayload ->
            validationInProgress.value = false

            goToNextStep(fixedPayload.fee, fixedPayload.collator)
        }
    }

    private fun goToNextStep(fee: Fee, collator: MythosCollator) = launch {
        val nextScreenPayload = ConfirmUnbondMythosPayload(
            collator = collator.toParcel(),
            amount = stakedAmount.first(),
            fee = fee.toParcel()
        )

        router.openUnbondConfirm(nextScreenPayload)
    }
}
