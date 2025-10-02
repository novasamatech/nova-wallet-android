package io.novafoundation.nova.feature_governance_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.updaters.GovernanceUpdateSystem
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.BlockTimeUpdater
import io.novafoundation.nova.runtime.network.updaters.InactiveIssuanceUpdater
import io.novafoundation.nova.runtime.network.updaters.SharedAssetBlockNumberUpdater
import io.novafoundation.nova.runtime.network.updaters.TotalIssuanceUpdater
import io.novafoundation.nova.runtime.network.updaters.multiChain.AsSharedStateUpdater
import io.novafoundation.nova.runtime.network.updaters.multiChain.DelegateToTimeLineChainUpdater
import io.novafoundation.nova.runtime.network.updaters.multiChain.DelegateToTimelineChainIdHolder
import io.novafoundation.nova.runtime.storage.SampledBlockTimeStorage
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class GovernanceUpdatersModule {

    @Provides
    @FeatureScope
    fun provideUpdateSystem(
        totalIssuanceUpdater: TotalIssuanceUpdater,
        inactiveIssuanceUpdater: InactiveIssuanceUpdater,
        blockNumberUpdater: SharedAssetBlockNumberUpdater,
        blockTimeUpdater: BlockTimeUpdater,
        chainRegistry: ChainRegistry,
        singleAssetSharedState: GovernanceSharedState,
        storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    ): UpdateSystem = GovernanceUpdateSystem(
        governanceUpdaters = listOf(
            AsSharedStateUpdater(totalIssuanceUpdater),
            AsSharedStateUpdater(inactiveIssuanceUpdater),
            DelegateToTimeLineChainUpdater(blockNumberUpdater),
            DelegateToTimeLineChainUpdater(blockTimeUpdater),
        ),
        chainRegistry = chainRegistry,
        governanceSharedState = singleAssetSharedState,
        storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory,
    )

    @Provides
    @FeatureScope
    fun blockTimeUpdater(
        chainIdHolder: DelegateToTimelineChainIdHolder,
        chainRegistry: ChainRegistry,
        sampledBlockTimeStorage: SampledBlockTimeStorage,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorage: StorageDataSource,
    ) = BlockTimeUpdater(chainIdHolder, chainRegistry, sampledBlockTimeStorage, remoteStorage)

    @Provides
    @FeatureScope
    fun provideBlockNumberUpdater(
        chainRegistry: ChainRegistry,
        crowdloanSharedState: GovernanceSharedState,
        storageCache: StorageCache,
    ) = SharedAssetBlockNumberUpdater(chainRegistry, crowdloanSharedState, storageCache)

    @Provides
    @FeatureScope
    fun provideTotalInsuranceUpdater(
        sharedState: GovernanceSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = TotalIssuanceUpdater(
        sharedState,
        storageCache,
        chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideInactiveInsuranceUpdater(
        sharedState: GovernanceSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = InactiveIssuanceUpdater(
        sharedState,
        storageCache,
        chainRegistry
    )
}
