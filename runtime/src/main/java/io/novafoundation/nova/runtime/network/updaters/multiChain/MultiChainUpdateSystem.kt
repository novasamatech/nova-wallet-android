package io.novafoundation.nova.runtime.network.updaters.multiChain

import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.updaters.ChainUpdaterGroupUpdateSystem
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState.SupportedAssetOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn

abstract class MultiChainUpdateSystem<E>(
    private val chainRegistry: ChainRegistry,
    private val singleAssetSharedState: SelectedAssetOptionSharedState<E>,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : ChainUpdaterGroupUpdateSystem(chainRegistry, storageSharedRequestsBuilderFactory) {

    abstract fun getUpdaters(option: SupportedAssetOption<E>): MultiMap<ChainId, Updater<*>>

    private val updateFlow = singleAssetSharedState.selectedOption.flatMapLatest { selectedOption ->
        val updatersByChain = getUpdaters(selectedOption)

        updatersByChain.map { (syncChainId, updaters) ->
            val syncChain = chainRegistry.getChain(syncChainId)
            runUpdaters(syncChain, updaters)
        }.mergeIfMultiple()
    }.shareIn(CoroutineScope(Dispatchers.IO), replay = 1, started = SharingStarted.WhileSubscribed())

    override fun start(): Flow<Updater.SideEffect> = updateFlow
}


class GroupBySyncChainMultiChainUpdateSystem<E>(
    chainRegistry: ChainRegistry,
    singleAssetSharedState: SelectedAssetOptionSharedState<E>,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val updaters: List<SharedStateBasedUpdater<*>>
) : MultiChainUpdateSystem<E>(chainRegistry, singleAssetSharedState, storageSharedRequestsBuilderFactory) {

    override fun getUpdaters(option: SupportedAssetOption<E>): MultiMap<ChainId, Updater<*>> {
        return updaters.groupBySyncingChain(option.assetWithChain.chain)
    }
}
