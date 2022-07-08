package io.novafoundation.nova.feature_staking_impl.di.staking

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.StakingUpdateSystem
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.Parachain
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.ParachainStakingUpdatersModule
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.turing.Turing
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.turing.TuringStakingUpdatersModule
import io.novafoundation.nova.feature_staking_impl.di.staking.relaychain.Relaychain
import io.novafoundation.nova.feature_staking_impl.di.staking.relaychain.RelaychainStakingUpdatersModule
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.BlockNumberUpdater
import io.novafoundation.nova.runtime.network.updaters.BlockTimeUpdater
import io.novafoundation.nova.runtime.storage.SampledBlockTimeStorage
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module(
    includes = [
        RelaychainStakingUpdatersModule::class,
        ParachainStakingUpdatersModule::class,
        TuringStakingUpdatersModule::class
    ]
)
class UpdatersModule {

    @Provides
    @FeatureScope
    fun provideStakingUpdateSystem(
        @Relaychain relaychainUpdaters: List<@JvmSuppressWildcards Updater>,
        @Parachain parachainUpdaters: List<@JvmSuppressWildcards Updater>,
        @Turing turingUpdaters: List<@JvmSuppressWildcards Updater>,
        blockTimeUpdater: BlockTimeUpdater,
        blockNumberUpdater: BlockNumberUpdater,
        chainRegistry: ChainRegistry,
        singleAssetSharedState: StakingSharedState
    ) = StakingUpdateSystem(
        relaychainUpdaters = relaychainUpdaters,
        parachainUpdaters = parachainUpdaters,
        commonUpdaters = listOf(blockTimeUpdater, blockNumberUpdater),
        chainRegistry = chainRegistry,
        singleAssetSharedState = singleAssetSharedState,
        turingExtraUpdaters = turingUpdaters
    )

    @Provides
    @FeatureScope
    fun blockTimeUpdater(
        singleAssetSharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
        sampledBlockTimeStorage: SampledBlockTimeStorage,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorage: StorageDataSource,
    ) = BlockTimeUpdater(singleAssetSharedState, chainRegistry, sampledBlockTimeStorage, remoteStorage)

    @Provides
    @FeatureScope
    fun provideBlockNumberUpdater(
        chainRegistry: ChainRegistry,
        crowdloanSharedState: StakingSharedState,
        storageCache: StorageCache,
    ) = BlockNumberUpdater(chainRegistry, crowdloanSharedState, storageCache)
}
