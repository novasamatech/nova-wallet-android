package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.mythos

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.unbonding.MythosUnbondingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.mythos.loadUserStakeState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.from
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class MythosUnbondingComponentFactory(
    private val mythosSharedComputation: MythosSharedComputation,
    private val interactor: MythosUnbondingInteractor,
    private val router: MythosStakingRouter,
    private val amountFormatter: AmountFormatter,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext,
    ): UnbondingComponent = MythosUnbondingComponent(
        stakingOption = stakingOption,
        hostContext = hostContext,
        mythosSharedComputation = mythosSharedComputation,
        interactor = interactor,
        router = router,
        amountFormatter = amountFormatter
    )
}

private class MythosUnbondingComponent(
    private val mythosSharedComputation: MythosSharedComputation,
    private val interactor: MythosUnbondingInteractor,
    private val amountFormatter: AmountFormatter,

    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
    private val router: MythosStakingRouter
) : UnbondingComponent,
    ComputationalScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<UnbondingEvent>>()

    override val state = mythosSharedComputation.loadUserStakeState(
        hostContext = hostContext,
        stateProducer = ::unbondingState
    )
        .shareInBackground()

    override fun onAction(action: UnbondingAction) {
        when (action) {
            UnbondingAction.RebondClicked -> Unit // rebond is not supported in mythos
            UnbondingAction.RedeemClicked -> router.openRedeem()
        }
    }

    private fun unbondingState(delegatorState: MythosDelegatorState.Staked): Flow<UnbondingState> {
        return combine(
            interactor.unbondingsFlow(delegatorState, stakingOption),
            hostContext.assetFlow,
        ) { unbondings, asset ->
            UnbondingState.from(unbondings, asset, amountFormatter)
        }
    }
}
