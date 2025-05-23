package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.ChainUpdaterGroupUpdateSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn


class StakingUpdateSystem(
    private val chainRegistry: ChainRegistry,
    private val stakingUpdaters: StakingUpdaters,
    private val singleAssetSharedState: StakingSharedState,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : ChainUpdaterGroupUpdateSystem(chainRegistry, storageSharedRequestsBuilderFactory) {

    private val updateFlow = singleAssetSharedState.selectedOption.flatMapLatest { selectedOption ->
        val stakingChain = selectedOption.assetWithChain.chain
        val stakingType = selectedOption.additional.stakingType

        val updatersByChain = stakingUpdaters.getUpdaters(stakingChain, stakingType)

        updatersByChain.map { (syncChainId, updaters) ->
            val syncChain = chainRegistry.getChain(syncChainId)
            runUpdaters(syncChain, updaters)
        }.mergeIfMultiple()
    }.shareIn(CoroutineScope(Dispatchers.IO), replay = 1, started = SharingStarted.WhileSubscribed())

    override fun start(): Flow<Updater.SideEffect> = updateFlow
}
