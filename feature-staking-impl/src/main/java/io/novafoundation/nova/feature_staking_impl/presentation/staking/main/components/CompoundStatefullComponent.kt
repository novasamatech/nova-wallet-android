package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.asLiveData
import io.novafoundation.nova.common.utils.childScope
import io.novafoundation.nova.common.utils.switchMap
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.SingleAssetSharedState.AssetWithChain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

typealias ComponentCreator<S, E, A> = (AssetWithChain, hostContext: ComponentHostContext) -> StatefullComponent<S, E, A>

class CompoundStakingComponentFactory(
    private val singleAssetSharedState: SingleAssetSharedState,
) {

    fun <S, E, A> create(
        relaychainComponentCreator: ComponentCreator<S, E, A>,
        parachainComponentCreator: ComponentCreator<S, E, A>,
        hostContext: ComponentHostContext,
    ): StatefullComponent<S, E, A> = CompoundStakingComponent(
        relaychainComponentCreator = relaychainComponentCreator,
        parachainComponentCreator = parachainComponentCreator,
        singleAssetSharedState = singleAssetSharedState,
        hostContext = hostContext
    )
}

private class CompoundStakingComponent<S, E, A>(
    singleAssetSharedState: SingleAssetSharedState,

    private val relaychainComponentCreator: ComponentCreator<S, E, A>,
    private val parachainComponentCreator: ComponentCreator<S, E, A>,
    private val hostContext: ComponentHostContext,
) : StatefullComponent<S, E, A>, CoroutineScope by hostContext.scope, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    private val childScope = hostContext.scope.childScope(supervised = true)
    private val childHostContext = hostContext.copy(scope = childScope)

    private val delegateFlow = singleAssetSharedState.assetWithChain.mapLatest {
        childScope.coroutineContext.cancelChildren() // cancel current component

        createDelegate(it)
    }.shareInBackground()

    override val events: LiveData<Event<E>> = delegateFlow
        .asLiveData(this)
        .switchMap { it.events }

    override val state: Flow<S?> = delegateFlow
        .flatMapLatest { it.state }
        .shareInBackground()

    override fun onAction(action: A) {
        launch {
            delegateFlow.first().onAction(action)
        }
    }

    private fun createDelegate(assetWithChain: AssetWithChain): StatefullComponent<S, E, A> {
        return when (assetWithChain.asset.staking) {
            Chain.Asset.StakingType.UNSUPPORTED -> UnsupportedComponent()
            Chain.Asset.StakingType.RELAYCHAIN -> relaychainComponentCreator(assetWithChain, childHostContext)
            Chain.Asset.StakingType.PARACHAIN -> parachainComponentCreator(assetWithChain, childHostContext)
        }
    }
}
