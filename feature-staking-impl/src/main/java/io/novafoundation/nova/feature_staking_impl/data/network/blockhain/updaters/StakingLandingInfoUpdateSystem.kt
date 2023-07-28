package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.updaters.ChainUpdaterGroupUpdateSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn

class StakingLandingInfoUpdateSystemFactory(
    private val stakingUpdaters: StakingUpdaters,
    private val chainRegistry: ChainRegistry,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) {

    fun create(chainWithAssetFlow: Flow<ChainWithAsset>): StakingLandingInfoUpdateSystem {
        return StakingLandingInfoUpdateSystem(
            stakingUpdaters,
            chainWithAssetFlow,
            chainRegistry,
            storageSharedRequestsBuilderFactory,
        )
    }
}

class StakingLandingInfoUpdateSystem(
    private val stakingUpdaters: StakingUpdaters,
    private val chainWithAssetFlow: Flow<ChainWithAsset>,
    chainRegistry: ChainRegistry,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : ChainUpdaterGroupUpdateSystem(chainRegistry, storageSharedRequestsBuilderFactory) {

    override fun start(): Flow<Updater.SideEffect> = chainWithAssetFlow.flatMapLatest { chainWithAsset ->
        val chain = chainWithAsset.chain

        val updaters = getUpdaters(chainWithAsset.asset)

        runUpdaters(chain, updaters)
    }.flowOn(Dispatchers.Default)

    private fun getUpdaters(asset: Chain.Asset): Collection<Updater> {
        return stakingUpdaters.getUpdaters(asset.supportedStakingOptions())
    }
}
