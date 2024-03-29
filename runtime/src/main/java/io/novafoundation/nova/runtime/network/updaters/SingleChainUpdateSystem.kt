package io.novafoundation.nova.runtime.network.updaters

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState.SupportedAssetOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn

abstract class SingleChainUpdateSystem<A>(
    chainRegistry: ChainRegistry,
    private val singleAssetSharedState: SelectedAssetOptionSharedState<A>,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : ChainUpdaterGroupUpdateSystem(chainRegistry, storageSharedRequestsBuilderFactory) {

    abstract fun getUpdaters(selectedAssetOption: SupportedAssetOption<A>): Collection<Updater<*>>

    private val updateFlow = singleAssetSharedState.selectedOption.flatMapLatest { selectedOption ->
        val chain = selectedOption.assetWithChain.chain

        val updaters = getUpdaters(selectedOption)

        runUpdaters(chain, updaters)
    }.shareIn(CoroutineScope(Dispatchers.IO), replay = 1, started = SharingStarted.WhileSubscribed())

    override fun start(): Flow<Updater.SideEffect> = updateFlow
}

class ConstantSingleChainUpdateSystem(
    private val updaters: List<Updater<*>>,
    chainRegistry: ChainRegistry,
    singleAssetSharedState: SelectedAssetOptionSharedState<*>,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : SingleChainUpdateSystem<Any?>(chainRegistry, singleAssetSharedState, storageSharedRequestsBuilderFactory) {

    override fun getUpdaters(selectedAssetOption: SupportedAssetOption<Any?>): List<Updater<*>> {
        return updaters
    }
}
