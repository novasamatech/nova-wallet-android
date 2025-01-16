package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.switchMap
import io.novafoundation.nova.common.utils.withItemScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.MYTHOS
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.NOMINATION_POOLS
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.TURING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

typealias ComponentCreator<S, E, A> = (StakingOption, hostContext: ComponentHostContext) -> StatefullComponent<S, E, A>

class CompoundStakingComponentFactory(
    private val stakingSharedState: StakingSharedState,
) {

    fun <S, E, A> create(
        relaychainComponentCreator: ComponentCreator<S, E, A>,
        parachainComponentCreator: ComponentCreator<S, E, A>,
        turingComponentCreator: ComponentCreator<S, E, A> = parachainComponentCreator,
        nominationPoolsCreator: ComponentCreator<S, E, A>,
        mythosCreator: ComponentCreator<S, E, A>,
        hostContext: ComponentHostContext,
    ): StatefullComponent<S, E, A> = CompoundStakingComponent(
        relaychainComponentCreator = relaychainComponentCreator,
        parachainComponentCreator = parachainComponentCreator,
        turingComponentCreator = turingComponentCreator,
        nominationPoolsCreator = nominationPoolsCreator,
        singleAssetSharedState = stakingSharedState,
        mythosCreator = mythosCreator,
        hostContext = hostContext
    )
}

private class CompoundStakingComponent<S, E, A>(
    singleAssetSharedState: StakingSharedState,

    private val relaychainComponentCreator: ComponentCreator<S, E, A>,
    private val parachainComponentCreator: ComponentCreator<S, E, A>,
    private val turingComponentCreator: ComponentCreator<S, E, A>,
    private val nominationPoolsCreator: ComponentCreator<S, E, A>,
    private val mythosCreator: ComponentCreator<S, E, A>,
    private val hostContext: ComponentHostContext,
) : StatefullComponent<S, E, A>, CoroutineScope by hostContext.scope, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    private val delegateFlow = singleAssetSharedState.selectedOption
        .withItemScope(parentScope = hostContext.scope)
        .map { (stakingOption, itemScope) ->
            val childHostContext = hostContext.copy(scope = ComputationalScope(itemScope))
            createDelegate(stakingOption, childHostContext)
        }.shareInBackground()

    override val events: LiveData<Event<E>> = delegateFlow
        .asLiveData()
        .switchMap { it.events }

    override val state: Flow<S?> = delegateFlow
        .flatMapLatest { it.state }
        .shareInBackground()

    override fun onAction(action: A) {
        launch {
            delegateFlow.first().onAction(action)
        }
    }

    private fun createDelegate(stakingOption: StakingOption, childHostContext: ComponentHostContext): StatefullComponent<S, E, A> {
        return when (stakingOption.additional.stakingType) {
            UNSUPPORTED -> UnsupportedComponent()
            RELAYCHAIN, RELAYCHAIN_AURA, ALEPH_ZERO -> relaychainComponentCreator(stakingOption, childHostContext)
            PARACHAIN -> parachainComponentCreator(stakingOption, childHostContext)
            TURING -> turingComponentCreator(stakingOption, childHostContext)
            NOMINATION_POOLS -> nominationPoolsCreator(stakingOption, childHostContext)
            MYTHOS -> mythosCreator(stakingOption, childHostContext)
        }
    }
}
