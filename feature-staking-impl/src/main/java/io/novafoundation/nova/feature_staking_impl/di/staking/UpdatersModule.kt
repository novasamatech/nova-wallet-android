package io.novafoundation.nova.feature_staking_impl.di.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdateSystem
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdaters
import io.novafoundation.nova.feature_staking_impl.di.staking.mythos.Mythos
import io.novafoundation.nova.feature_staking_impl.di.staking.mythos.MythosStakingUpdatersModule
import io.novafoundation.nova.feature_staking_impl.di.staking.nominationPool.NominationPoolStakingUpdatersModule
import io.novafoundation.nova.feature_staking_impl.di.staking.nominationPool.NominationPools
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.Parachain
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.ParachainStakingUpdatersModule
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.turing.Turing
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.turing.TuringStakingUpdatersModule
import io.novafoundation.nova.feature_staking_impl.di.staking.relaychain.Relaychain
import io.novafoundation.nova.feature_staking_impl.di.staking.relaychain.RelaychainStakingUpdatersModule
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.BlockTimeUpdater
import io.novafoundation.nova.runtime.network.updaters.SharedAssetBlockNumberUpdater
import io.novafoundation.nova.runtime.network.updaters.TotalIssuanceUpdater
import io.novafoundation.nova.runtime.network.updaters.multiChain.AsSharedStateUpdater
import io.novafoundation.nova.runtime.network.updaters.multiChain.DelegateToTimeLineChainUpdater
import io.novafoundation.nova.runtime.network.updaters.multiChain.DelegateToTimelineChainIdHolder
import io.novafoundation.nova.runtime.storage.SampledBlockTimeStorage
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named
import javax.inject.Qualifier

@Module(
    includes = [
        RelaychainStakingUpdatersModule::class,
        ParachainStakingUpdatersModule::class,
        TuringStakingUpdatersModule::class,
        NominationPoolStakingUpdatersModule::class,
        MythosStakingUpdatersModule::class
    ]
)
class UpdatersModule {

    @Provides
    @CommonUpdaters
    @FeatureScope
    fun provideCommonUpdaters(
        blockTimeUpdater: BlockTimeUpdater,
        blockNumberUpdater: SharedAssetBlockNumberUpdater,
        totalIssuanceUpdater: TotalIssuanceUpdater
    ) = StakingUpdaters.Group(
        DelegateToTimeLineChainUpdater(blockTimeUpdater),
        DelegateToTimeLineChainUpdater(blockNumberUpdater),
        AsSharedStateUpdater(totalIssuanceUpdater)
    )

    @Provides
    @FeatureScope
    fun provideStakingUpdaters(
        @Relaychain relaychainUpdaters: StakingUpdaters.Group,
        @Parachain parachainUpdaters: StakingUpdaters.Group,
        @Turing turingUpdaters: StakingUpdaters.Group,
        @NominationPools nominationPoolsUpdaters: StakingUpdaters.Group,
        @Mythos mythosUpdaters: StakingUpdaters.Group,
        @CommonUpdaters commonUpdaters: StakingUpdaters.Group
    ): StakingUpdaters {
        return StakingUpdaters(
            relaychainUpdaters = relaychainUpdaters,
            parachainUpdaters = parachainUpdaters,
            commonUpdaters = commonUpdaters,
            turingExtraUpdaters = turingUpdaters,
            nominationPoolsUpdaters = nominationPoolsUpdaters,
            mythosUpdaters = mythosUpdaters
        )
    }

    @Provides
    @FeatureScope
    fun provideStakingUpdateSystem(
        stakingUpdaters: StakingUpdaters,
        chainRegistry: ChainRegistry,
        singleAssetSharedState: StakingSharedState,
        storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    ) = StakingUpdateSystem(
        stakingUpdaters = stakingUpdaters,
        chainRegistry = chainRegistry,
        stakingSharedState = singleAssetSharedState,
        storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory
    )

    @Provides
    @FeatureScope
    fun blockTimeUpdater(
        timelineDelegatingChainIdHolder: DelegateToTimelineChainIdHolder,
        chainRegistry: ChainRegistry,
        sampledBlockTimeStorage: SampledBlockTimeStorage,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorage: StorageDataSource,
    ) = BlockTimeUpdater(timelineDelegatingChainIdHolder, chainRegistry, sampledBlockTimeStorage, remoteStorage)

    @Provides
    @FeatureScope
    fun provideBlockNumberUpdater(
        chainRegistry: ChainRegistry,
        timelineDelegatingChainIdHolder: DelegateToTimelineChainIdHolder,
        storageCache: StorageCache,
    ) = SharedAssetBlockNumberUpdater(chainRegistry, timelineDelegatingChainIdHolder, storageCache)

    @Provides
    @FeatureScope
    fun provideTotalInsuranceUpdater(
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ) = TotalIssuanceUpdater(
        sharedState,
        storageCache,
        chainRegistry
    )
}

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class CommonUpdaters
