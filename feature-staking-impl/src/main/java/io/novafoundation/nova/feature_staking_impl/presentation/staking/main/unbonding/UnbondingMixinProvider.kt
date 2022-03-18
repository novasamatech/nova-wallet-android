package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.unbonding

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnboningsdState
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.mainStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.unbonding.rebond.RebondKind
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.ConfirmRebondPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigInteger

class UnbondingMixinFactory(
    private val unbondInteractor: UnbondInteractor,
    private val validationExecutor: ValidationExecutor,
    private val actionAwaitableFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager,
    private val rebondValidationSystem: StakeActionsValidationSystem,
    private val redeemValidationSystem: StakeActionsValidationSystem,
    private val router: StakingRouter,
) {

    fun create(
        errorDisplayer: (Throwable) -> Unit,
        stashState: StakingState.Stash,
        assetFlow: Flow<Asset>,
        coroutineScope: CoroutineScope
    ): UnbondingMixin.Presentation = UnbondingMixinProvider(
        unbondInteractor = unbondInteractor,
        validationExecutor = validationExecutor,
        actionAwaitableFactory = actionAwaitableFactory,
        resourceManager = resourceManager,
        rebondValidationSystem = rebondValidationSystem,
        redeemValidationSystem = redeemValidationSystem,
        router = router,
        errorDisplayer = errorDisplayer,
        stashState = stashState,
        assetFlow = assetFlow,
        coroutineScope = coroutineScope
    )
}

private class UnbondingMixinProvider(
    private val unbondInteractor: UnbondInteractor,
    private val validationExecutor: ValidationExecutor,
    private val actionAwaitableFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager,
    private val rebondValidationSystem: StakeActionsValidationSystem,
    private val redeemValidationSystem: StakeActionsValidationSystem,
    private val router: StakingRouter,
    // From Parent Component
    private val errorDisplayer: (Throwable) -> Unit,
    private val stashState: StakingState.Stash,
    private val assetFlow: Flow<Asset>,
    coroutineScope: CoroutineScope,
) : UnbondingMixin.Presentation,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val rebondKindAwaitable = actionAwaitableFactory.create<Unit, RebondKind>()

    private val unbondingsFlow = unbondInteractor.unbondingsFlow(stashState)
        .inBackground()
        .share()

    override val state: Flow<UnbondingMixin.State> = combine(assetFlow, unbondingsFlow) { asset, unbondings ->
        createUiState(unbondings, asset)
    }
        .inBackground()
        .share()

    override fun redeemClicked() = requireValidManageAction(redeemValidationSystem) {
        router.openRedeem()
    }

    override fun cancelClicked() = requireValidManageAction(rebondValidationSystem) {
        launch {
            when (rebondKindAwaitable.awaitAction()) {
                RebondKind.LAST -> openConfirmRebond(unbondInteractor::newestUnbondingAmount)
                RebondKind.ALL -> openConfirmRebond(unbondInteractor::allUnbondingsAmount)
                RebondKind.CUSTOM -> router.openCustomRebond()
            }
        }
    }

    private suspend fun openConfirmRebond(amountBuilder: (List<Unbonding>) -> BigInteger) {
        val unbondingsState = unbondingsFlow.first()
        val asset = assetFlow.first()

        val amountInPlanks = amountBuilder(unbondingsState.unbondings)
        val amount = asset.token.amountFromPlanks(amountInPlanks)

        router.openConfirmRebond(ConfirmRebondPayload(amount))
    }

    private fun createUiState(unbondingsState: UnboningsdState, asset: Asset): UnbondingMixin.State {
        return if (unbondingsState.unbondings.isEmpty()) {
            UnbondingMixin.State.Empty
        } else {
            UnbondingMixin.State.HaveUnbondings(
                redeemEnabled = unbondingsState.anythingToRedeem,
                cancelEnabled = unbondingsState.anythingToUnbond,
                unbondings = unbondingsState.unbondings.mapIndexed { idx, unbonding ->
                    mapUnbondingToUnbondingModel(idx, unbonding, asset)
                }
            )
        }
    }

    private fun mapUnbondingToUnbondingModel(index: Int, unbonding: Unbonding, asset: Asset): UnbondingModel {
        return UnbondingModel(
            index = index,
            status = unbonding.status,
            amountModel = mapAmountToAmountModel(unbonding.amount, asset)
        )
    }

    private fun requireValidManageAction(
        validationSystem: StakeActionsValidationSystem,
        block: (StakeActionsValidationPayload) -> Unit,
    ) {
        launch {
            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = StakeActionsValidationPayload(stashState),
                errorDisplayer = errorDisplayer,
                validationFailureTransformerDefault = { mainStakingValidationFailure(it, resourceManager) },
                block = block
            )
        }
    }
}
