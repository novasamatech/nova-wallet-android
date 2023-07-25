package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.nominationPools

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.unbondings.NominationPoolUnbondingsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.nominationPools.loadPoolMemberState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.UnbondingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class NominationPoolsUnbondingComponentFactory(
    private val poolMemberUseCase: NominationPoolMemberUseCase,
    private val interactor: NominationPoolUnbondingsInteractor,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext,
    ): UnbondingComponent = NominationPoolsUnbondingComponent(
        poolMemberUseCase = poolMemberUseCase,
        interactor = interactor,
        hostContext = hostContext,
        stakingOption = stakingOption
    )
}

private class NominationPoolsUnbondingComponent(
    private val poolMemberUseCase: NominationPoolMemberUseCase,
    private val interactor: NominationPoolUnbondingsInteractor,

    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
) : UnbondingComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<UnbondingEvent>>()

    override val state = poolMemberUseCase.loadPoolMemberState(hostContext, ::loadUnbondings)
        .shareInBackground()

    override fun onAction(action: UnbondingAction) {
        launch {
            when (action) {
                UnbondingAction.RebondClicked -> {} // rebond is not supported in nomination pools
                UnbondingAction.RedeemClicked -> redeemClicked()
            }
        }
    }

    private fun loadUnbondings(poolMember: PoolMember): Flow<UnbondingState> {
        return combine(
            interactor.unbondingsFlow(poolMember, stakingOption, hostContext.scope),
            hostContext.assetFlow,
        ) { unbondings, asset ->
            UnbondingState.from(unbondings, asset)
        }
    }

    fun redeemClicked() {
        // TODO
    }
}
