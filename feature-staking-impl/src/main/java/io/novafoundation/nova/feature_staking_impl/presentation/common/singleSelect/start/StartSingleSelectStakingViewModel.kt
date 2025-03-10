package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.start

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.WithAccountId
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.findById
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.SingleSelectRecommendator
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.model.TargetWithStakedAmount
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsResponse
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.SelectStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.rewards.SingleSelectStakingRewardEstimationComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.start.StartSingleSelectStakingViewModel.StartSingleSelectStakingLogic
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

abstract class StartSingleSelectStakingViewModel<T, L : StartSingleSelectStakingLogic<T>>(
    logicFactory: StartSingleSelectStakingLogic.Factory<T, L>,
    feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    private val rewardsComponentFactory: SingleSelectStakingRewardEstimationComponentFactory,
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    protected val validationExecutor: ValidationExecutor,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val recommendatorFactory: SingleSelectRecommendator.Factory<T>,
    private val selectedAssetState: StakingSharedState,
    private val router: ReturnableRouter,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Validatable by validationExecutor
    where T : Identifiable, T : WithAccountId {

    @Suppress("LeakingThis")
    protected val logic: L = logicFactory.create(this)

    abstract val hintsMixin: HintsMixin

    private val selectRecommendator by lazyAsync {
        recommendatorFactory.create(selectedAssetState.selectedOption(), computationalScope = this)
    }

    protected val validationInProgress = MutableStateFlow(false)

    protected val assetFlow = assetUseCase.currentAssetFlow()
        .share()

    private val chainAssetFlow = selectedAssetState.selectedAssetFlow()

    private val stakeableAmount = logic.stakeableAmount(assetFlow)
        .shareInBackground()

    val originFeeMixin = feeLoaderMixinV2Factory.createDefault(viewModelScope, chainAssetFlow)

    private val maxAmountProvider = MaxActionProvider.create(viewModelScope) {
        chainAssetFlow.providingBalance(stakeableAmount)
            .deductFee(originFeeMixin)
    }

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        maxActionProvider = maxAmountProvider
    )

    private val isStakeMoreFlow = logic.isStakeMore()

    private val alreadyStakedTargetsFlow = logic.alreadyStakedTargets()
        .shareInBackground()

    private val selectedTargetFlow = MutableStateFlow<T?>(null)

    private val selectedTargetIdFlow = selectedTargetFlow.map { it?.accountId }
        .distinctUntilChanged()

    private val selectedTargetWithStake = selectedTargetFlow.filterNotNull().flatMapLatest { selectedTarget ->
        logic.alreadyStakedAmountTo(selectedTarget.accountId).map {
            TargetWithStakedAmount(it, selectedTarget)
        }
    }

    @Suppress("UNCHECKED_CAST")
    val selectedTargetModelFlow = combine(selectedTargetWithStake, assetFlow, logic::mapStakedTargetToUi)
        .onStart<SelectStakeTargetModel<T>?> { emit(null) }
        .shareInBackground()

    private val alreadyStakedAmountToSelected = selectedTargetFlow.transformLatest { selectedTarget ->
        if (selectedTarget == null) {
            emit(BigInteger.ZERO)
            return@transformLatest
        }

        emitAll(logic.alreadyStakedAmountTo(selectedTarget.accountId))
    }

    private val resultingStakedAmountFlow = combine(
        alreadyStakedAmountToSelected,
        amountChooserMixin.amount,
        assetFlow
    ) { currentDelegationInPlanks, enteredAmount, asset ->
        val currentDelegationAmount = asset.token.amountFromPlanks(currentDelegationInPlanks)

        currentDelegationAmount + enteredAmount
    }

    val chooseTargetAction = actionAwaitableMixinFactory.create<ChooseStakeTargetActionPayload<T>, ChooseStakedStakeTargetsResponse<T>>()

    val minimumStake = selectedTargetFlow.map {
        val minimumStake = logic.minimumStakeToGetRewards(it)
        val asset = assetFlow.first()

        mapAmountToAmountModel(minimumStake, asset)
    }.shareInBackground()

    val rewardsComponent = rewardsComponentFactory.create(
        computationalScope = this,
        assetFlow = assetFlow,
        selectedAmount = resultingStakedAmountFlow,
        selectedTarget = selectedTargetIdFlow
    )

    val title = combine(assetFlow, isStakeMoreFlow) { asset, isStakeMore ->
        if (isStakeMore) {
            resourceManager.getString(R.string.staking_bond_more_v1_9_0)
        } else {
            resourceManager.getString(R.string.staking_stake_format, asset.token.configuration.symbol)
        }
    }
        .shareInBackground()

    val buttonState = combine(
        validationInProgress,
        selectedTargetFlow,
        amountChooserMixin.inputState
    ) { validationInProgress, collator, amountInput ->
        when {
            validationInProgress -> DescriptiveButtonState.Loading
            collator == null -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.staking_parachain_select_collator_hint))
            amountInput.value.isEmpty() -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }

    init {
        originFeeMixin.connectWith(
            inputSource1 = amountChooserMixin.backPressuredPlanks,
            inputSource2 = selectedTargetIdFlow,
            feeConstructor = { _, amount, selectedTargetId ->
                val chain = selectedAssetState.chain()
                val stakeTargetId = selectedTargetId ?: chain.emptyAccountId().intoKey()

                logic.estimateFee(amount, stakeTargetId)
            },
        )

        listenSelectedTargetChanges()
        setInitialTarget()
    }

    protected abstract suspend fun openSelectNewTarget()

    protected abstract suspend fun openSelectFirstTarget()

    protected abstract suspend fun goNext(
        target: T,
        amount: BigDecimal,
        fee: Fee,
        asset: Asset
    )

    fun selectTargetClicked() = launch {
        val alreadyStakedCollators = alreadyStakedTargetsFlow.first()
        val selected = selectedTargetFlow.first()
        val asset = assetFlow.first()

        if (alreadyStakedCollators.isEmpty()) {
            openSelectFirstTarget()
        } else {
            val payload = createSelectTargetPayload(alreadyStakedCollators, selected, asset)

            when (val response = chooseTargetAction.awaitAction(payload)) {
                ChooseStakedStakeTargetsResponse.New -> openSelectNewTarget()
                is ChooseStakedStakeTargetsResponse.Existing -> selectedTargetFlow.value = response.target
            }
        }
    }

    fun nextClicked() = launchUnit {
        val selectedTarget = selectedTargetFlow.first() ?: return@launchUnit

        validationInProgress.value = true

        val asset = assetFlow.first()
        val amount = amountChooserMixin.amount.first()
        val fee = originFeeMixin.awaitFee()

        goNext(selectedTarget, amount, fee, asset)
    }

    fun backClicked() {
        router.back()
    }

    private suspend fun createSelectTargetPayload(
        targetWithStakedAmounts: List<TargetWithStakedAmount<T>>,
        selected: T?,
        asset: Asset,
    ): ChooseStakeTargetActionPayload<T> {
        return withContext(Dispatchers.Default) {
            val collatorModels = targetWithStakedAmounts.map {
                logic.mapStakedTargetToUi(it, asset)
            }

            val selectedModel = collatorModels.findById(selected)

            ChooseStakedStakeTargetsBottomSheet.Payload(collatorModels, selectedModel)
        }
    }

    private fun setInitialTarget() = launch {
        val isStakeMore = isStakeMoreFlow.first()

        if (isStakeMore) {
            val alreadyStakedTargets = alreadyStakedTargetsFlow.first()
            selectedTargetFlow.value = alreadyStakedTargets.first().target
        } else {
            selectedTargetFlow.value = selectRecommendator.await().defaultRecommendation()
        }
    }

    private fun listenSelectedTargetChanges() {
        logic.selectedTargetChanges()
            .onEach { selectedTargetFlow.value = it }
            .inBackground()
            .launchIn(this)
    }

    interface StartSingleSelectStakingLogic<T : Identifiable> {

        fun interface Factory<T : Identifiable, L : StartSingleSelectStakingLogic<T>> {

            fun create(computationalScope: ComputationalScope): L
        }

        fun selectedTargetChanges(): Flow<T>

        fun stakeableAmount(assetFlow: Flow<Asset>): Flow<Balance>

        fun isStakeMore(): Flow<Boolean>

        fun alreadyStakedTargets(): Flow<List<TargetWithStakedAmount<T>>>

        fun alreadyStakedAmountTo(accountIdKey: AccountIdKey): Flow<Balance>

        suspend fun mapStakedTargetToUi(
            target: TargetWithStakedAmount<T>,
            asset: Asset
        ): SelectStakeTargetModel<T>

        suspend fun minimumStakeToGetRewards(selectedStakeTarget: T?): Balance

        suspend fun estimateFee(amount: Balance, targetId: AccountIdKey): Fee
    }
}

private typealias ChooseStakeTargetActionPayload<T> = ChooseStakedStakeTargetsBottomSheet.Payload<SelectStakeTargetModel<T>>
