package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.parachain

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.withFlagSet
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.unbondings.DelegationRequestWithCollatorInfo
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.unbondings.ParachainStakingUnbondingsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.SelectStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.awaitAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.parachainStaking.loadDelegatingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.from
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState.AssetWithChain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParachainUnbondingComponentFactory(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val interactor: ParachainStakingUnbondingsInteractor,
    private val router: ParachainStakingRouter,
) {

    fun create(
        assetWithChain: AssetWithChain,
        hostContext: ComponentHostContext,
    ): UnbondingComponent = ParachainUnbondingComponent(
        assetWithChain = assetWithChain,
        hostContext = hostContext,
        delegatorStateUseCase = delegatorStateUseCase,
        interactor = interactor,
        router = router,
        addressIconGenerator = addressIconGenerator
    )
}

private class ParachainUnbondingComponent(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val interactor: ParachainStakingUnbondingsInteractor,
    private val addressIconGenerator: AddressIconGenerator,

    private val assetWithChain: AssetWithChain,
    private val hostContext: ComponentHostContext,
    private val router: ParachainStakingRouter
) : UnbondingComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<UnbondingEvent>>()

    override val state = delegatorStateUseCase.loadDelegatingState(
        hostContext = hostContext,
        assetWithChain = assetWithChain,
        stateProducer = ::delegatorSummaryStateFlow
    )
        .shareInBackground()

    override fun onAction(action: UnbondingAction) {
        when (action) {
            UnbondingAction.RebondClicked -> handleRebond()
            UnbondingAction.RedeemClicked -> router.openRedeem()
        }
    }

    private val cancelLoadingFlow = MutableStateFlow(false)

    private fun handleRebond() = launch {

        val delegatorState = delegatorStateUseCase.currentDelegatorState().castOrNull<DelegatorState.Delegator>() ?: return@launch
        val payload = cancelLoadingFlow.withFlagSet { createRebondChooserPayload(delegatorState) }

        val selected = events.awaitAction(payload, UnbondingEvent::ChooseRebondTarget).payload as DelegationRequestWithCollatorInfo

        hostContext.errorDisplayer(NotImplementedError("TODO - show rebond for ${selected.collatorIdentity?.display}"))
    }

    private suspend fun createRebondChooserPayload(
        delegatorState: DelegatorState.Delegator
    ) = withContext(Dispatchers.Default) {
        val unbondingRequests = interactor.pendingUnbondings(delegatorState)
        val asset = hostContext.assetFlow.first()

        val selectStakeTargetModels = unbondingRequests.map { unbondingWithCollator ->
            SelectStakeTargetModel(
                addressModel = addressIconGenerator.createAccountAddressModel(
                    chain = delegatorState.chain,
                    accountId = unbondingWithCollator.request.collator,
                    name = unbondingWithCollator.collatorIdentity?.display
                ),
                amount = mapAmountToAmountModel(unbondingWithCollator.request.action.amount, asset),
                payload = unbondingWithCollator,
                amountLabelRes = R.string.wallet_balance_unbonding_v1_9_0,
                active = true
            )
        }

        ChooseStakedStakeTargetsBottomSheet.Payload(
            data = selectStakeTargetModels,
            selected = null,
            titleRes = R.string.staking_rebond
        )
    }

    private fun delegatorSummaryStateFlow(delegatorState: DelegatorState.Delegator): Flow<UnbondingState> {
        return combine(
            interactor.unbondingsFlow(delegatorState),
            hostContext.assetFlow,
            cancelLoadingFlow
        ) { unbondings, asset, cancelLoading ->
            UnbondingState.from(unbondings, asset, cancelLoading)
        }
    }
}
