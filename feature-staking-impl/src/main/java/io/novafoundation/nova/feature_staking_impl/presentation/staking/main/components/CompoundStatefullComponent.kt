package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.switchMap
import io.novafoundation.nova.common.utils.withItemScope
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.TURING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.assetWithChain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

typealias ComponentCreator<S, E, A> = (ChainWithAsset, hostContext: ComponentHostContext) -> StatefullComponent<S, E, A>

class CompoundStakingComponentFactory(
    private val singleAssetSharedState: SelectedAssetOptionSharedState<*>,
) {

    fun <S, E, A> create(
        relaychainComponentCreator: ComponentCreator<S, E, A>,
        parachainComponentCreator: ComponentCreator<S, E, A>,
        turingComponentCreator: ComponentCreator<S, E, A> = parachainComponentCreator,
        hostContext: ComponentHostContext,
    ): StatefullComponent<S, E, A> = CompoundStakingComponent(
        relaychainComponentCreator = relaychainComponentCreator,
        parachainComponentCreator = parachainComponentCreator,
        turingComponentCreator = turingComponentCreator,
        singleAssetSharedState = singleAssetSharedState,
        hostContext = hostContext
    )
}

private class CompoundStakingComponent<S, E, A>(
    singleAssetSharedState: SelectedAssetOptionSharedState<*>,

    private val relaychainComponentCreator: ComponentCreator<S, E, A>,
    private val parachainComponentCreator: ComponentCreator<S, E, A>,
    private val turingComponentCreator: ComponentCreator<S, E, A>,
    private val hostContext: ComponentHostContext,
) : StatefullComponent<S, E, A>, CoroutineScope by hostContext.scope, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    private val delegateFlow = singleAssetSharedState.assetWithChain
        .withItemScope(parentScope = hostContext.scope)
        .map { (chainWithAsset, itemScope) ->
            val childHostContext = hostContext.copy(scope = itemScope)
            createDelegate(chainWithAsset, childHostContext)
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

    private fun createDelegate(assetWithChain: ChainWithAsset, childHostContext: ComponentHostContext): StatefullComponent<S, E, A> {
        // TODO staking dashboard - switch by selected staking option
        return when (assetWithChain.asset.staking.firstOrNull()) {
            null, UNSUPPORTED -> UnsupportedComponent()
            RELAYCHAIN, RELAYCHAIN_AURA, ALEPH_ZERO -> relaychainComponentCreator(assetWithChain, childHostContext)
            PARACHAIN -> parachainComponentCreator(assetWithChain, childHostContext)
            TURING -> turingComponentCreator(assetWithChain, childHostContext)
        }
    }
}
