package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.setup

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.selectingOneOf
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.ParachainStakingUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators.mapCollatorToSelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.SelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.hints.ParachainStakingUnbondHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.parachainStakingUnbondPayloadAutoFix
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.parachainStakingUnbondValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.transferableAmountModel
import jp.co.soramitsu.fearless_utils.extensions.fromHex
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
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val collatorsUseCase: CollatorsUseCase,
    private val hintsMixinFactory: ParachainStakingUnbondHintsMixinFactory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin {

    private val validationInProgress = MutableStateFlow(false)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .share()

    private val currentDelegatorStateFlow = delegatorStateUseCase.currentDelegatorStateFlow()
        .shareInBackground()

    private val alreadyStakedCollatorsFlow = currentDelegatorStateFlow
        .mapLatest(collatorsUseCase::getSelectedCollators)
        .shareInBackground()

    private val selectedCollatorFlow = singleReplaySharedFlow<Collator>()
    private val selectedCollatorIdFlow = selectedCollatorFlow.map { it.accountIdHex.fromHex() }

    private val stakedAmount = combine(
        currentDelegatorStateFlow,
        selectedCollatorIdFlow
    ) { delegatorState, collatorId ->
        delegatorState.delegationAmountTo(collatorId).orZero()
    }

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        availableBalanceFlow = stakedAmount,
        balanceLabel = R.string.staking_main_stake_balance_staked
    )

    val selectedCollatorModel = combine(
        selectedCollatorFlow,
        currentDelegatorStateFlow,
        assetFlow
    ) { selectedCollator, currentDelegatorState, asset ->
        mapCollatorToSelectCollatorModel(selectedCollator, currentDelegatorState, asset, addressIconGenerator)
    }.shareInBackground()

    val chooseCollatorAction = actionAwaitableMixinFactory.selectingOneOf<SelectCollatorModel>()

    val minimumStake = selectedCollatorFlow.map {
        val minimumStake = it.minimumStakeToGetRewards ?: collatorsUseCase.defaultMinimumStake()
        val asset = assetFlow.first()

        mapAmountToAmountModel(minimumStake, asset)
    }.shareInBackground()

    val transferable = assetFlow.map(Asset::transferableAmountModel)
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
        feeLoaderMixin.connectWith(
            inputSource1 = amountChooserMixin.backPressuredAmount,
            inputSource2 = selectedCollatorIdFlow,
            scope = this,
            feeConstructor = { amount, collatorId -> interactor.estimateFee(amount.toPlanks(), collatorId) },
            onRetryCancelled = ::backClicked
        )

        setInitialCollator()
    }

    fun selectCollatorClicked() = launch {
        val delegatorState = currentDelegatorStateFlow.first()
        val alreadyStakedCollators = alreadyStakedCollatorsFlow.first()

        val asset = assetFlow.first()
        val selectedCollator = selectedCollatorFlow.first()

        val payload = withContext(Dispatchers.Default) {
            val collatorModels = alreadyStakedCollators.map { mapCollatorToSelectCollatorModel(it, delegatorState, asset, addressIconGenerator) }
            val selected = collatorModels.find { it.collator.accountIdHex == selectedCollator.accountIdHex }

            DynamicListBottomSheet.Payload(collatorModels, selected)
        }

        val newCollator = chooseCollatorAction.awaitAction(payload)
        setCollatorIfCanUnbond(newCollator, delegatorState)
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    private suspend fun setCollatorIfCanUnbond(newCollator: SelectCollatorModel, delegatorState: DelegatorState) {
        val collarAccountId = newCollator.collator.accountIdHex.fromHex()

        if (interactor.canUnbond(collarAccountId, delegatorState)) {
            selectedCollatorFlow.emit(newCollator.collator)
        } else {
            showError(
                title = resourceManager.getString(R.string.staking_parachain_unbond_already_exists_title),
                text = resourceManager.getString(R.string.staking_parachain_unbond_already_exists_message)
            )
        }
    }

    private fun setInitialCollator() = launch {
        val alreadyStakedCollators = alreadyStakedCollatorsFlow.first()

        if (alreadyStakedCollators.isNotEmpty()) {
            selectedCollatorFlow.emit(alreadyStakedCollators.first())
        }
    }

    private fun maybeGoToNext() = requireFee { fee ->
        launch {
            val payload = ParachainStakingUnbondPayload(
                amount = amountChooserMixin.amount.first(),
                fee = fee,
                asset = assetFlow.first(),
                collator = selectedCollatorFlow.first()
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { parachainStakingUnbondValidationFailure(it, resourceManager) },
                autoFixPayload = ::parachainStakingUnbondPayloadAutoFix,
                progressConsumer = validationInProgress.progressConsumer()
            ) {
                validationInProgress.value = false

                goToNextStep(fee = fee, amount = payload.amount, collator = payload.collator)
            }
        }
    }

    private fun goToNextStep(
        fee: BigDecimal,
        amount: BigDecimal,
        collator: Collator,
    ) = launch {
        showMessage("Ready to open confirm screen")
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )
}
