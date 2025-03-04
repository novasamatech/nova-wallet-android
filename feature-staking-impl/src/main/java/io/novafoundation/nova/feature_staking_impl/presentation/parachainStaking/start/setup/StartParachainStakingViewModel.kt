package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.delegationAmountTo
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.stakeablePlanks
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.model.TargetWithStakedAmount
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.DelegationsLimit
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.StartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.SelectStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.start.StartSingleSelectStakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenRequester
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.openRequest
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorParcelModelToCollator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorToCollatorParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.selectCollators.mapSelectedCollatorToSelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.hints.ConfirmStartParachainStakingHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.ParachainStakingRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.startParachainStakingValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class StartParachainStakingViewModel(
    private val router: ParachainStakingRouter,
    private val selectCollatorInterScreenRequester: SelectCollatorInterScreenRequester,
    private val interactor: StartParachainStakingInteractor,
    private val rewardsComponentFactory: ParachainStakingRewardsComponentFactory,
    private val addressIconGenerator: AddressIconGenerator,
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    validationExecutor: ValidationExecutor,
    private val validationSystem: StartParachainStakingValidationSystem,
    feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val collatorsUseCase: CollatorsUseCase,
    private val payload: StartParachainStakingPayload,
    private val collatorRecommendatorFactory: CollatorRecommendatorFactory,
    private val selectedAssetState: StakingSharedState,
    hintsMixinFactory: ConfirmStartParachainStakingHintsMixinFactory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : StartSingleSelectStakingViewModel<Collator, StartParachainStakingViewModel.ParachainLogic>(
    logicFactory = { scope ->
        ParachainLogic(
            scope,
            selectCollatorInterScreenRequester,
            delegatorStateUseCase,
            collatorsUseCase,
            selectedAssetState,
            addressIconGenerator,
            resourceManager,
            interactor
        )
    },
    rewardsComponentFactory = rewardsComponentFactory,
    assetUseCase = assetUseCase,
    resourceManager = resourceManager,
    validationExecutor = validationExecutor,
    feeLoaderMixinV2Factory = feeLoaderMixinV2Factory,
    actionAwaitableMixinFactory = actionAwaitableMixinFactory,
    recommendatorFactory = collatorRecommendatorFactory,
    selectedAssetState = selectedAssetState,
    router = router,
    amountChooserMixinFactory = amountChooserMixinFactory,
) {

    override val hintsMixin = hintsMixinFactory.create(coroutineScope = this, payload.flowMode)

    override suspend fun openSelectNewTarget() {
        val delegatorState = logic.currentDelegatorStateFlow.first()

        when (val check = interactor.checkDelegationsLimit(delegatorState)) {
            DelegationsLimit.NotReached -> selectCollatorInterScreenRequester.openRequest()
            is DelegationsLimit.Reached -> {
                showError(
                    title = resourceManager.getString(R.string.staking_parachain_max_delegations_title),
                    text = resourceManager.getString(R.string.staking_parachain_max_delegations_message, check.limit)
                )
            }
        }
    }

    override suspend fun openSelectFirstTarget() {
        selectCollatorInterScreenRequester.openRequest()
    }

    override suspend fun goNext(target: Collator, amount: BigDecimal, fee: Fee, asset: Asset) {
        val payload = StartParachainStakingValidationPayload(
            amount = amount,
            fee = fee,
            asset = assetFlow.first(),
            collator = target,
            delegatorState = logic.currentDelegatorStateFlow.first(),
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { startParachainStakingValidationFailure(it, resourceManager) },
            progressConsumer = validationInProgress.progressConsumer()
        ) {
            validationInProgress.value = false

            goToNextStep(fee = it.fee, amount = amount, collator = target)
        }
    }

    private fun goToNextStep(
        fee: Fee,
        amount: BigDecimal,
        collator: Collator,
    ) = launch {
        val payload = withContext(Dispatchers.Default) {
            ConfirmStartParachainStakingPayload(
                collator = mapCollatorToCollatorParcelModel(collator),
                amount = fee.asset.planksFromAmount(amount),
                fee = mapFeeToParcel(fee),
                flowMode = payload.flowMode
            )
        }

        router.openConfirmStartStaking(payload)
    }

    class ParachainLogic(
        computationalScope: ComputationalScope,
        private val selectCollatorInterScreenRequester: SelectCollatorInterScreenRequester,
        private val delegatorStateUseCase: DelegatorStateUseCase,
        private val collatorsUseCase: CollatorsUseCase,
        private val selectedAssetState: StakingSharedState,
        private val addressIconGenerator: AddressIconGenerator,
        private val resourceManager: ResourceManager,
        private val interactor: StartParachainStakingInteractor,
    ) : StartSingleSelectStakingLogic<Collator>,
        ComputationalScope by computationalScope {

        val currentDelegatorStateFlow = delegatorStateUseCase.currentDelegatorStateFlow()
            .shareInBackground()

        override fun selectedTargetChanges(): Flow<Collator> {
            return selectCollatorInterScreenRequester.responseFlow.map { response ->
                mapCollatorParcelModelToCollator(response.collator)
            }
        }

        override fun stakeableAmount(assetFlow: Flow<Asset>): Flow<Balance> {
            return combine(assetFlow, currentDelegatorStateFlow) { asset, currentDelegator ->
                currentDelegator.stakeablePlanks(asset.freeInPlanks)
            }
        }

        override fun isStakeMore(): Flow<Boolean> {
            return currentDelegatorStateFlow.map { it is DelegatorState.Delegator }
        }

        override fun alreadyStakedTargets(): Flow<List<TargetWithStakedAmount<Collator>>> {
            return currentDelegatorStateFlow
                .mapLatest(collatorsUseCase::getSelectedCollators)
        }

        override fun alreadyStakedAmountTo(accountIdKey: AccountIdKey): Flow<Balance> {
            return currentDelegatorStateFlow.map {
                it.delegationAmountTo(accountIdKey.value).orZero()
            }
        }

        override suspend fun mapStakedTargetToUi(target: TargetWithStakedAmount<Collator>, asset: Asset): SelectStakeTargetModel<Collator> {
            return mapSelectedCollatorToSelectCollatorModel(
                selectedCollator = target,
                chain = selectedAssetState.chain(),
                asset = asset,
                addressIconGenerator = addressIconGenerator,
                resourceManager = resourceManager
            )
        }

        override suspend fun minimumStakeToGetRewards(selectedStakeTarget: Collator?): Balance {
            return selectedStakeTarget?.minimumStakeToGetRewards ?: collatorsUseCase.defaultMinimumStake()
        }

        override suspend fun estimateFee(amount: Balance, targetId: AccountIdKey): Fee {
            return interactor.estimateFee(amount, targetId.value)
        }
    }
}
