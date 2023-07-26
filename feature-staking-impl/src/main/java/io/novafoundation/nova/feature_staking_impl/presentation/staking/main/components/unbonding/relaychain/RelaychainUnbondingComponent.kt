package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.relaychain

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.common.presentation.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.firstNotNull
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.awaitAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.from
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.rebond.RebondKind
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.mainStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm.ConfirmRebondPayload
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.math.BigInteger

const val REBOND_KIND_ALL = "All"
const val REBOND_KIND_LAST = "Last"
const val REBOND_KIND_CUSTOM = "Custom"

class RelaychainUnbondingComponentFactory(
    private val unbondInteractor: UnbondInteractor,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val rebondValidationSystem: StakeActionsValidationSystem,
    private val redeemValidationSystem: StakeActionsValidationSystem,
    private val router: StakingRouter,
    private val stakingSharedComputation: StakingSharedComputation,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext,
    ): UnbondingComponent = RelaychainUnbondingComponent(
        unbondInteractor = unbondInteractor,
        validationExecutor = validationExecutor,
        resourceManager = resourceManager,
        rebondValidationSystem = rebondValidationSystem,
        redeemValidationSystem = redeemValidationSystem,
        router = router,
        hostContext = hostContext,
        stakingSharedComputation = stakingSharedComputation,
        stakingOption = stakingOption
    )
}

private class RelaychainUnbondingComponent(
    private val stakingSharedComputation: StakingSharedComputation,
    private val unbondInteractor: UnbondInteractor,
    private val validationExecutor: ValidationExecutor,
    private val rebondValidationSystem: StakeActionsValidationSystem,
    private val redeemValidationSystem: StakeActionsValidationSystem,
    private val router: StakingRouter,
    private val resourceManager: ResourceManager,

    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
) : UnbondingComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<UnbondingEvent>>()

    private val selectedAccountStakingStateFlow = stakingSharedComputation.selectedAccountStakingStateFlow(
        assetWithChain = stakingOption.assetWithChain,
        scope = hostContext.scope
    )

    private val unbondingsFlow = selectedAccountStakingStateFlow.transformLatest {
        if (it !is StakingState.Stash) {
            emit(null)
        } else {
            emitAll(unbondInteractor.unbondingsFlow(it, hostContext.scope).withLoading())
        }
    }
        .shareInBackground()

    override val state = combine(hostContext.assetFlow, unbondingsFlow) { asset, unbondingsLoading ->
        unbondingsLoading?.let {
            it.map { unbonding ->
                UnbondingState.from(unbonding, asset)
            }
        }
    }
        .onStart { emit(null) }
        .shareInBackground()

    override fun onAction(action: UnbondingAction) {
        launch {
            when (action) {
                UnbondingAction.RebondClicked -> rebondClicked()
                UnbondingAction.RedeemClicked -> redeemClicked()
            }
        }
    }

    private fun rebondClicked() = requireValidManageAction(rebondValidationSystem) {
        val chosenKind = events.awaitAction(rebondKinds(), UnbondingEvent::ChooseRebondKind)

        when (chosenKind.id) {
            REBOND_KIND_ALL -> openConfirmRebond(unbondInteractor::allUnbondingsAmount)
            REBOND_KIND_LAST -> openConfirmRebond(unbondInteractor::newestUnbondingAmount)
            REBOND_KIND_CUSTOM -> router.openCustomRebond()
        }
    }

    fun redeemClicked() = requireValidManageAction(redeemValidationSystem) {
        router.openRedeem()
    }

    private fun rebondKinds() = listOf(
        RebondKind(REBOND_KIND_ALL, resourceManager.getString(R.string.staking_rebond_all)),
        RebondKind(REBOND_KIND_LAST, resourceManager.getString(R.string.staking_rebond_last)),
        RebondKind(REBOND_KIND_CUSTOM, resourceManager.getString(R.string.staking_rebond_custom)),
    )

    private suspend fun openConfirmRebond(amountBuilder: (List<Unbonding>) -> BigInteger) {
        val unbondingsState = unbondingsFlow.map { it?.dataOrNull }.firstNotNull()
        val asset = hostContext.assetFlow.first()

        val amountInPlanks = amountBuilder(unbondingsState.unbondings)
        val amount = asset.token.amountFromPlanks(amountInPlanks)

        router.openConfirmRebond(ConfirmRebondPayload(amount))
    }

    private fun requireValidManageAction(
        validationSystem: StakeActionsValidationSystem,
        block: suspend (StakeActionsValidationPayload) -> Unit,
    ) {
        launch {
            val stashState = selectedAccountStakingStateFlow.filterIsInstance<StakingState.Stash>().first()

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = StakeActionsValidationPayload(stashState),
                errorDisplayer = hostContext.errorDisplayer,
                validationFailureTransformerDefault = { mainStakingValidationFailure(it, resourceManager) },
                block = { launch { block(it) } }
            )
        }
    }
}
