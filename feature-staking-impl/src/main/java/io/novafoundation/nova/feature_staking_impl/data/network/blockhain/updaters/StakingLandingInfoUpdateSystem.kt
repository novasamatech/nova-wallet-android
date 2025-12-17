package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.updaters.ChainUpdaterGroupUpdateSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class StakingLandingInfoUpdateSystemFactory(
    private val stakingUpdaters: StakingUpdaters,
    private val chainRegistry: ChainRegistry,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) {

    fun create(chainId: ChainId, stakingTypes: List<Chain.Asset.StakingType>): StakingLandingInfoUpdateSystem {
        return StakingLandingInfoUpdateSystem(
            stakingUpdaters,
            chainId,
            stakingTypes,
            chainRegistry,
            storageSharedRequestsBuilderFactory,
        )
    }
}

class StakingLandingInfoUpdateSystem(
    private val stakingUpdaters: StakingUpdaters,
    private val chainId: ChainId,
    private val stakingTypes: List<Chain.Asset.StakingType>,
    private val chainRegistry: ChainRegistry,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : ChainUpdaterGroupUpdateSystem(chainRegistry, storageSharedRequestsBuilderFactory) {

    override fun start(): Flow<Updater.SideEffect> = flowOfAll {
        val stakingChain = chainRegistry.getChain(chainId)

        val updatersBySyncChainId = stakingUpdaters.getUpdaters(stakingChain, stakingTypes)

        updatersBySyncChainId.map { (syncChainId, updaters) ->
            val syncChain = chainRegistry.getChain(syncChainId)
            runUpdaters(syncChain, updaters)
        }.mergeIfMultiple()
    }.flowOn(Dispatchers.Default)
}
