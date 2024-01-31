package io.novafoundation.nova.feature_staking_impl.di.staking.relaychain.dock

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.rpc.DockRpc
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.rpc.RealDockRpc
import io.novafoundation.nova.feature_staking_impl.data.repository.DockStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealDockStakingRepository
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class DockStakingModule {

    @Provides
    @FeatureScope
    fun provideDockRpc(chainRegistry: ChainRegistry): DockRpc = RealDockRpc(chainRegistry)

    @Provides
    @FeatureScope
    fun provideDockRepository(
        dockRpc: DockRpc,
        @Named(REMOTE_STORAGE_SOURCE) storageDataSource: StorageDataSource
    ): DockStakingRepository = RealDockStakingRepository(dockRpc, storageDataSource)
}
