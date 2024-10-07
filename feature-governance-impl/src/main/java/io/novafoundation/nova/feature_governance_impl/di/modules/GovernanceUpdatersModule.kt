package io.novafoundation.nova.feature_governance_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SharedAssetBlockNumberUpdater
import io.novafoundation.nova.runtime.network.updaters.BlockTimeUpdater
import io.novafoundation.nova.runtime.network.updaters.ConstantSingleChainUpdateSystem
import io.novafoundation.nova.runtime.network.updaters.InactiveIssuanceUpdater
import io.novafoundation.nova.runtime.network.updaters.TotalIssuanceUpdater
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
    ): UpdateSystem = ConstantSingleChainUpdateSystem(
        updaters = listOf(totalIssuanceUpdater, inactiveIssuanceUpdater, blockNumberUpdater, blockTimeUpdater),
        chainRegistry = chainRegistry,
        singleAssetSharedState = singleAssetSharedState,
        storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory,
    )

    @Provides
    @FeatureScope
    fun blockTimeUpdater(
        singleAssetSharedState: GovernanceSharedState,
        chainRegistry: ChainRegistry,
        sampledBlockTimeStorage: SampledBlockTimeStorage,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorage: StorageDataSource,
    ) = BlockTimeUpdater(singleAssetSharedState, chainRegistry, sampledBlockTimeStorage, remoteStorage)

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
