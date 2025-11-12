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
import io.novafoundation.nova.runtime.network.updaters.multiChain.DelegateToTimeLineChainUpdater
import io.novafoundation.nova.runtime.network.updaters.multiChain.DelegateToTimelineChainIdHolder
import io.novafoundation.nova.runtime.network.updaters.multiChain.GroupBySyncChainMultiChainUpdateSystem

@Module
class CrowdloanUpdatersModule {

    @Provides
    @FeatureScope
    fun provideTimelineDelegatingHolder(sharedState: CrowdloanSharedState) = DelegateToTimelineChainIdHolder(sharedState)

    @Provides
    @FeatureScope
    fun provideBlockNumberUpdater(
        chainRegistry: ChainRegistry,
        chainIdHolder: DelegateToTimelineChainIdHolder,
        storageCache: StorageCache,
    ) = SharedAssetBlockNumberUpdater(chainRegistry, chainIdHolder, storageCache)

    @Provides
    @FeatureScope
    fun provideCrowdloanUpdateSystem(
        chainRegistry: ChainRegistry,
        crowdloanSharedState: CrowdloanSharedState,
        blockNumberUpdater: SharedAssetBlockNumberUpdater,
        storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    ): UpdateSystem = GroupBySyncChainMultiChainUpdateSystem(
        updaters = listOf(
            DelegateToTimeLineChainUpdater(blockNumberUpdater)
        ),
        chainRegistry = chainRegistry,
        singleAssetSharedState = crowdloanSharedState,
        storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory
    )
}
