package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.StartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenRequester
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.openRequest
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorParcelModelToCollator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorToCollatorParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.ChooseCollatorResponse
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.SelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.RealParachainStakingRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.connectWith
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.startParachainStakingValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class StartParachainStakingViewModel(
    private val router: ParachainStakingRouter,
    private val selectCollatorInterScreenRequester: SelectCollatorInterScreenRequester,
    private val interactor: StartParachainStakingInteractor,
    private val rewardsComponentFactory: RealParachainStakingRewardsComponentFactory,
    private val singleAssetSharedState: SingleAssetSharedState,
    private val addressIconGenerator: AddressIconGenerator,
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: StartParachainStakingValidationSystem,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin {

    private val validationInProgress = MutableStateFlow(false)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .share()

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        balanceField = Asset::transferable,
        balanceLabel = R.string.wallet_balance_transferable
    )

    private val currentDelegatorStateFlow = delegatorStateUseCase.currentDelegatorStateFlow()
        .shareInBackground()

    private val isStakeMore = currentDelegatorStateFlow.map { it is DelegatorState.Delegator }

    private val alreadyStakedCollatorsFlow = currentDelegatorStateFlow
        .mapLatest(interactor::getSelectedCollators)
        .shareInBackground()

    private val selectedCollatorFlow = MutableStateFlow<Collator?>(null)
    private val selectedCollatorIdFlow = selectedCollatorFlow.map { it?.accountIdHex?.fromHex() }

    val selectedCollatorModel = combine(
        selectedCollatorFlow,
        currentDelegatorStateFlow,
        assetFlow
    ) { selectedCollator, currentDelegatorState, asset ->
        selectedCollator?.let { mapCollatorToSelectCollatorModel(it, currentDelegatorState, asset) }
    }.shareInBackground()

    private val resultingStakedAmountFlow = combine(
        currentDelegatorStateFlow,
        selectedCollatorFlow,
        amountChooserMixin.amount,
        assetFlow
    ) { delegatorState, selectedCollator, enteredAmount, asset ->
        val currentDelegationInPlanks = selectedCollator?.let {
            val collatorId = it.accountIdHex.fromHex()
            delegatorState.delegationAmountTo(collatorId)
        }.orZero()

        val currentDelegationAmount = asset.token.amountFromPlanks(currentDelegationInPlanks)

        currentDelegationAmount + enteredAmount
    }

    val chooseCollatorAction = actionAwaitableMixinFactory.create<DynamicListBottomSheet.Payload<SelectCollatorModel>, ChooseCollatorResponse>()

    val minimumStake = selectedCollatorFlow.map {
        val minimumStake = it?.minimumStakeToGetRewards ?: interactor.defaultMinimumStake()
        val asset = assetFlow.first()

        mapAmountToAmountModel(minimumStake, asset)
    }.shareInBackground()

    val rewardsComponent = rewardsComponentFactory.create(
        parentScope = this,
        assetFlow = assetFlow
    )

    val title = combine(assetFlow, isStakeMore) { asset, isStakeMore ->
        if (isStakeMore) {
            resourceManager.getString(R.string.staking_bond_more_v1_9_0)
        } else {
            resourceManager.getString(R.string.staking_stake_format, asset.token.configuration.symbol)
        }
    }
        .shareInBackground()

    val buttonState = combine(
        validationInProgress,
        selectedCollatorFlow,
        amountChooserMixin.amountInput
    ) { validationInProgress, collator, amountInput ->
        when {
            validationInProgress -> DescriptiveButtonState.Loading
            collator == null -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.staking_parachain_select_collator_hint))
            amountInput.isEmpty() -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }

    init {
        rewardsComponent connectWith resultingStakedAmountFlow
        rewardsComponent connectWith selectedCollatorIdFlow

        feeLoaderMixin.connectWith(
            inputSource1 = amountChooserMixin.backPressuredAmount,
            inputSource2 = selectedCollatorIdFlow,
            scope = this,
            feeConstructor = { amount, collatorId -> interactor.estimateFee(amount.toPlanks(), collatorId) },
            onRetryCancelled = ::backClicked
        )

        listenCollatorChanges()
        setInitialCollator()
    }

    fun selectCollatorClicked() = launch {
        val delegatorState = currentDelegatorStateFlow.first()
        val alreadyStakedCollators = alreadyStakedCollatorsFlow.first()

        if (alreadyStakedCollators.isEmpty()) {
            selectCollatorInterScreenRequester.openRequest()
        } else {
            val asset = assetFlow.first()
            val selectedCollator = selectedCollatorFlow.first()
            val payload = withContext(Dispatchers.Default) {
                val collatorModels = alreadyStakedCollators.map { mapCollatorToSelectCollatorModel(it, delegatorState, asset) }
                val selected = collatorModels.find { it.collator.accountIdHex == selectedCollator?.accountIdHex }

                DynamicListBottomSheet.Payload(collatorModels, selected)
            }

            when(val response = chooseCollatorAction.awaitAction(payload)) {
                ChooseCollatorResponse.New -> selectCollatorInterScreenRequester.openRequest()
                is ChooseCollatorResponse.Existing -> selectedCollatorFlow.value = response.collatorModel.collator
            }
        }
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    private fun setInitialCollator() = launch {
        val alreadyStakedCollators = alreadyStakedCollatorsFlow.first()

        if (alreadyStakedCollators.isNotEmpty()) {
            selectedCollatorFlow.value = alreadyStakedCollators.first()
        }
    }

    private fun listenCollatorChanges() {
        selectCollatorInterScreenRequester.responseFlow.onEach { response ->
            val collator = mapCollatorParcelModelToCollator(response.collator)

            selectedCollatorFlow.value = collator
        }
            .inBackground()
            .launchIn(this)
    }

    private fun maybeGoToNext() = requireFee { fee ->
        launch {
            val collator = selectedCollatorFlow.first() ?: return@launch
            val amount = amountChooserMixin.amount.first()

            val payload = StartParachainStakingValidationPayload(
                amount = amount,
                fee = fee,
                asset = assetFlow.first(),
                collator = collator
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { startParachainStakingValidationFailure(it, resourceManager) },
                progressConsumer = validationInProgress.progressConsumer()
            ) {
                validationInProgress.value = false

                goToNextStep(fee = fee, amount = amount, collator = collator)
            }
        }
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

        router.openConfirmStartStaking(payload)
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )

    private suspend fun mapCollatorToSelectCollatorModel(
        collator: Collator,
        delegatorState: DelegatorState,
        asset: Asset,
    ): SelectCollatorModel {
        val chain = singleAssetSharedState.chain()

        val addressModel = addressIconGenerator.createAccountAddressModel(
            chain = chain,
            address = collator.address,
            name = collator.identity?.display
        )

        val collatorId = collator.accountIdHex.fromHex()
        val stakedAmount = delegatorState.castOrNull<DelegatorState.Delegator>()?.delegationAmountTo(collatorId)?.let {
            mapAmountToAmountModel(it, asset)
        }

        return SelectCollatorModel(
            addressModel = addressModel,
            staked = stakedAmount,
            collator = collator
        )
    }
}
