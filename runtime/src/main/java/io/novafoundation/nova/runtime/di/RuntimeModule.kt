package io.novafoundation.nova.runtime.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.dao.StorageDao
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicSerializers
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.extrinsic.MortalityConstructor
import io.novafoundation.nova.runtime.extrinsic.RealExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.DbRuntimeVersionsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.RemoteEventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.RuntimeVersionsRepository
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import io.novafoundation.nova.runtime.repository.RealParachainInfoRepository
import io.novafoundation.nova.runtime.repository.RealTotalIssuanceRepository
import io.novafoundation.nova.runtime.repository.RemoteTimestampRepository
import io.novafoundation.nova.runtime.repository.TimestampRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.storage.DbStorageCache
import io.novafoundation.nova.runtime.storage.PrefsSampledBlockTimeStorage
import io.novafoundation.nova.runtime.storage.SampledBlockTimeStorage
import io.novafoundation.nova.runtime.storage.source.LocalStorageSource
import io.novafoundation.nova.runtime.storage.source.RemoteStorageSource
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

const val LOCAL_STORAGE_SOURCE = "LOCAL_STORAGE_SOURCE"
const val REMOTE_STORAGE_SOURCE = "REMOTE_STORAGE_SOURCE"

@Module
class RuntimeModule {

    @Provides
    @ApplicationScope
    fun provideExtrinsicBuilderFactory(
        rpcCalls: RpcCalls,
        chainRegistry: ChainRegistry,
        mortalityConstructor: MortalityConstructor,
    ) = ExtrinsicBuilderFactory(
        rpcCalls,
        chainRegistry,
        mortalityConstructor
    )

    @Provides
    @ApplicationScope
    fun provideStorageCache(
        storageDao: StorageDao,
    ): StorageCache = DbStorageCache(storageDao)

    @Provides
    @Named(LOCAL_STORAGE_SOURCE)
    @ApplicationScope
    fun provideLocalStorageSource(
        chainRegistry: ChainRegistry,
        storageCache: StorageCache,
    ): StorageDataSource = LocalStorageSource(chainRegistry, storageCache)

    @Provides
    @Named(REMOTE_STORAGE_SOURCE)
    @ApplicationScope
    fun provideRemoteStorageSource(
        chainRegistry: ChainRegistry,
        bulkRetriever: BulkRetriever,
    ): StorageDataSource = RemoteStorageSource(chainRegistry, bulkRetriever)

    @Provides
    @ApplicationScope
    fun provideSampledBlockTimeStorage(
        gson: Gson,
        preferences: Preferences,
    ): SampledBlockTimeStorage = PrefsSampledBlockTimeStorage(gson, preferences)

    @Provides
    @ApplicationScope
    fun provideChainStateRepository(
        @Named(LOCAL_STORAGE_SOURCE) localStorageSource: StorageDataSource,
        sampledBlockTimeStorage: SampledBlockTimeStorage,
        chainRegistry: ChainRegistry
    ) = ChainStateRepository(localStorageSource, sampledBlockTimeStorage, chainRegistry)

    @Provides
    @ApplicationScope
    fun provideMortalityProvider(
        chainStateRepository: ChainStateRepository,
        rpcCalls: RpcCalls,
    ) = MortalityConstructor(rpcCalls, chainStateRepository)

    @Provides
    @ApplicationScope
    fun provideSubstrateCalls(
        chainRegistry: ChainRegistry
    ) = RpcCalls(chainRegistry)

    @Provides
    @ApplicationScope
    @ExtrinsicSerialization
    fun provideExtrinsicGson() = ExtrinsicSerializers.gson()

    @Provides
    @ApplicationScope
    fun provideRuntimeVersionsRepository(
        chainDao: ChainDao
    ): RuntimeVersionsRepository = DbRuntimeVersionsRepository(chainDao)

    @Provides
    @ApplicationScope
    fun provideEventsRepository(
        rpcCalls: RpcCalls,
        chainRegistry: ChainRegistry,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource
    ): EventsRepository = RemoteEventsRepository(rpcCalls, chainRegistry, remoteStorageSource)

    @Provides
    @ApplicationScope
    fun provideMultiChainQrSharingFactory() = MultiChainQrSharingFactory()

    @Provides
    @ApplicationScope
    fun provideParachainInfoRepository(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource
    ): ParachainInfoRepository = RealParachainInfoRepository(remoteStorageSource)

    @Provides
    @ApplicationScope
    fun provideExtrinsicValidityUseCase(
        mortalityConstructor: MortalityConstructor
    ): ExtrinsicValidityUseCase = RealExtrinsicValidityUseCase(mortalityConstructor)

    @Provides
    @ApplicationScope
    fun provideTimestampRepository(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
    ): TimestampRepository = RemoteTimestampRepository(remoteStorageSource)

    @Provides
    @ApplicationScope
    fun provideTotalIssuanceRepository(
        @Named(LOCAL_STORAGE_SOURCE) localStorageSource: StorageDataSource,
    ): TotalIssuanceRepository = RealTotalIssuanceRepository(localStorageSource)
}
