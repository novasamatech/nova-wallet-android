package io.novafoundation.nova.feature_crowdloan_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SharedAssetBlockNumberUpdater
import io.novafoundation.nova.runtime.network.updaters.ConstantSingleChainUpdateSystem

@Module
class CrowdloanUpdatersModule {

    @Provides
    @FeatureScope
    fun provideBlockNumberUpdater(
        chainRegistry: ChainRegistry,
        crowdloanSharedState: CrowdloanSharedState,
        storageCache: StorageCache,
    ) = SharedAssetBlockNumberUpdater(chainRegistry, crowdloanSharedState, storageCache)

    @Provides
    @FeatureScope
    fun provideCrowdloanUpdateSystem(
        chainRegistry: ChainRegistry,
        crowdloanSharedState: CrowdloanSharedState,
        blockNumberUpdater: SharedAssetBlockNumberUpdater,
        storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    ): UpdateSystem = ConstantSingleChainUpdateSystem(
        updaters = listOf(
            blockNumberUpdater
        ),
        chainRegistry = chainRegistry,
        singleAssetSharedState = crowdloanSharedState,
        storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory
    )
}
