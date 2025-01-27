package io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.setup

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.hints.NoHintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingBlockNumberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.model.TargetWithStakedAmount
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.isDelegating
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.stakeableBalance
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.recommendations.MythosCollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.StartMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.StartMythosStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.validations.StartMythosStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.DelegationsLimit
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.SelectStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.start.StartSingleSelectStakingViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.SelectMythosInterScreenRequester
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.MythosCollatorFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.openRequest
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.common.validations.MythosStartStakingValidationFailureFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.confirm.ConfirmStartMythosStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model.toDomain
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model.toParcelable
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.setup.rewards.MythosStakingRewardsComponentFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.toParcel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class SetupStartMythosStakingViewModel(
    private val router: MythosStakingRouter,
    rewardsComponentFactory: MythosStakingRewardsComponentFactory,
    assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    validationExecutor: ValidationExecutor,
    feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val collatorRecommendatorFactory: MythosCollatorRecommendatorFactory,
    private val mythosDelegatorStateUseCase: MythosDelegatorStateUseCase,
    private val selectedAssetState: StakingSharedState,
    private val validationSystem: StartMythosStakingValidationSystem,
    private val stakingBlockNumberUseCase: StakingBlockNumberUseCase,
    private val mythosStartStakingValidationFailureFormatter: MythosStartStakingValidationFailureFormatter,
    mythosSharedComputation: MythosSharedComputation,
    mythosCollatorFormatter: MythosCollatorFormatter,
    private val interactor: StartMythosStakingInteractor,
    private val selectCollatorInterScreenRequester: SelectMythosInterScreenRequester,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : StartSingleSelectStakingViewModel<MythosCollator, SetupStartMythosStakingViewModel.MythosLogic>(
    logicFactory = { scope ->
        MythosLogic(
            computationalScope = scope,
            mythosSharedComputation = mythosSharedComputation,
            mythosCollatorFormatter = mythosCollatorFormatter,
            interactor = interactor,
            mythosDelegatorStateUseCase = mythosDelegatorStateUseCase,
            selectCollatorRequester = selectCollatorInterScreenRequester,
            stakingBlockNumberUseCase = stakingBlockNumberUseCase
        )
    },
    rewardsComponentFactory = rewardsComponentFactory,
    assetUseCase = assetUseCase,
    resourceManager = resourceManager,
    validationExecutor = validationExecutor,
    feeLoaderMixin = feeLoaderMixin,
    actionAwaitableMixinFactory = actionAwaitableMixinFactory,
    recommendatorFactory = collatorRecommendatorFactory,
    selectedAssetState = selectedAssetState,
    router = router,
    amountChooserMixinFactory = amountChooserMixinFactory,
) {

    override val hintsMixin = NoHintsMixin()

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

    override suspend fun goNext(target: MythosCollator, amount: BigDecimal, fee: Fee, asset: Asset) {
        val payload = StartMythosStakingValidationPayload(
            amount = amount,
            fee = fee,
            asset = asset,
            collator = target,
            delegatorState = logic.currentDelegatorStateFlow.first(),
            currentBlockNumber = logic.currentBlockNumberFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { reason, _ -> mythosStartStakingValidationFailureFormatter.formatValidationFailure(reason) },
            progressConsumer = validationInProgress.progressConsumer()
        ) {
            validationInProgress.value = false

            goToNextStep(fee, asset.token.planksFromAmount(amount), target)
        }
    }

    private fun goToNextStep(
        fee: Fee,
        amount: Balance,
        collator: MythosCollator,
    ) {
        val payload = ConfirmStartMythosStakingPayload(collator.toParcelable(), amount, fee.toParcel())
        router.openConfirmStartStaking(payload)
    }

    class MythosLogic(
        computationalScope: ComputationalScope,
        private val mythosSharedComputation: MythosSharedComputation,
        private val mythosCollatorFormatter: MythosCollatorFormatter,
        private val mythosDelegatorStateUseCase: MythosDelegatorStateUseCase,
        private val interactor: StartMythosStakingInteractor,
        private val selectCollatorRequester: SelectMythosInterScreenRequester,
        private val stakingBlockNumberUseCase: StakingBlockNumberUseCase,
    ) : StartSingleSelectStakingLogic<MythosCollator>,
        ComputationalScope by computationalScope {

        val currentDelegatorStateFlow = mythosSharedComputation.delegatorStateFlow()
            .shareInBackground()

        val currentBlockNumberFlow = stakingBlockNumberUseCase.currentBlockNumberFlow()
            .shareInBackground()

        override fun selectedTargetChanges(): Flow<MythosCollator> {
            return selectCollatorRequester.responseFlow
                .map { it.toDomain() }
        }

        override fun stakeableAmount(assetFlow: Flow<Asset>): Flow<Balance> {
            return combine(
                assetFlow,
                currentDelegatorStateFlow,
                currentBlockNumberFlow
            ) { asset, mythosDelegatorState, currentBlockNumberFlow ->
                mythosDelegatorState.stakeableBalance(asset, currentBlockNumberFlow)
            }
        }

        override fun isStakeMore(): Flow<Boolean> {
            return currentDelegatorStateFlow.map { it.isDelegating() }
        }

        override fun alreadyStakedTargets(): Flow<List<TargetWithStakedAmount<MythosCollator>>> {
            return currentDelegatorStateFlow.map { mythosDelegatorStateUseCase.getStakedCollators(it) }
        }

        override fun alreadyStakedAmountTo(accountIdKey: AccountIdKey): Flow<Balance> {
            return currentDelegatorStateFlow.map {
                it.delegationAmountTo(accountIdKey).orZero()
            }
        }

        override suspend fun mapStakedTargetToUi(target: TargetWithStakedAmount<MythosCollator>, asset: Asset): SelectStakeTargetModel<MythosCollator> {
            return mythosCollatorFormatter.collatorToSelectUi(target, asset.token)
        }

        override suspend fun minimumStakeToGetRewards(selectedStakeTarget: MythosCollator?): Balance {
            return interactor.minStake()
        }

        override suspend fun estimateFee(amount: Balance, targetId: AccountIdKey): Fee {
            return interactor.estimateFee(currentDelegatorStateFlow.first(), targetId, amount)
        }
    }
}
