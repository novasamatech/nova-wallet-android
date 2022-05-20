package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.parachain

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.unbondings.ParachainStakingUnbondingsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.parachainStaking.loadDelegatingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.from
import io.novafoundation.nova.runtime.state.SingleAssetSharedState.AssetWithChain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ParachainUnbondingComponentFactory(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val interactor: ParachainStakingUnbondingsInteractor,
) {

    fun create(
        assetWithChain: AssetWithChain,
        hostContext: ComponentHostContext,
    ): UnbondingComponent = ParachainUnbondingComponent(
        assetWithChain = assetWithChain,
        hostContext = hostContext,
        delegatorStateUseCase = delegatorStateUseCase,
        interactor = interactor
    )
}

private class ParachainUnbondingComponent(
    delegatorStateUseCase: DelegatorStateUseCase,
    private val interactor: ParachainStakingUnbondingsInteractor,

    private val assetWithChain: AssetWithChain,
    private val hostContext: ComponentHostContext,
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
        // TODO
    }

    private fun delegatorSummaryStateFlow(delegatorState: DelegatorState.Delegator): Flow<UnbondingState> {
        return combine(
            interactor.unbondingsFlow(delegatorState),
            hostContext.assetFlow
        ) { unbondings, asset ->
            UnbondingState.from(unbondings, asset)
        }
    }
}
