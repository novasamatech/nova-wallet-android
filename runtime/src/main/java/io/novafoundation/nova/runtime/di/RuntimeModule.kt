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
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RealMultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ethereum.gas.GasPriceProviderFactory
import io.novafoundation.nova.runtime.ethereum.gas.RealGasPriceProviderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicSerializers
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.extrinsic.MortalityConstructor
import io.novafoundation.nova.runtime.extrinsic.RealExtrinsicValidityUseCase
import io.novafoundation.nova.runtime.extrinsic.multi.ExtrinsicSplitter
import io.novafoundation.nova.runtime.extrinsic.multi.RealExtrinsicSplitter
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.extrinsic.visitor.impl.RealExtrinsicWalk
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.RemoteToDomainChainMapperFacade
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.DbRuntimeVersionsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.RemoteEventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.RuntimeVersionsRepository
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.BlockLimitsRepository
import io.novafoundation.nova.runtime.repository.ChainNodeRepository
import io.novafoundation.nova.runtime.repository.ChainRepository
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import io.novafoundation.nova.runtime.repository.PreConfiguredChainsRepository
import io.novafoundation.nova.runtime.repository.RealBlockLimitsRepository
import io.novafoundation.nova.runtime.repository.RealChainNodeRepository
import io.novafoundation.nova.runtime.repository.RealChainRepository
import io.novafoundation.nova.runtime.repository.RealParachainInfoRepository
import io.novafoundation.nova.runtime.repository.RealPreConfiguredChainsRepository
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
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import javax.inject.Named
import javax.inject.Provider

const val LOCAL_STORAGE_SOURCE = "LOCAL_STORAGE_SOURCE"
const val REMOTE_STORAGE_SOURCE = "REMOTE_STORAGE_SOURCE"

const val BULK_RETRIEVER_PAGE_SIZE = 1000

@Module
class RuntimeModule {

    @Provides
    @ApplicationScope
    fun provideExtrinsicBuilderFactory(
        chainDao: ChainDao,
        rpcCalls: RpcCalls,
        chainRegistry: ChainRegistry,
        mortalityConstructor: MortalityConstructor,
    ) = ExtrinsicBuilderFactory(
        chainDao,
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
        sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    ): StorageDataSource = LocalStorageSource(chainRegistry, sharedRequestsBuilderFactory, storageCache)

    @Provides
    @ApplicationScope
    fun provideBulkRetriever(): BulkRetriever {
        return BulkRetriever(BULK_RETRIEVER_PAGE_SIZE)
    }

    @Provides
    @Named(REMOTE_STORAGE_SOURCE)
    @ApplicationScope
    fun provideRemoteStorageSource(
        chainRegistry: ChainRegistry,
        sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
        bulkRetriever: BulkRetriever,
    ): StorageDataSource = RemoteStorageSource(chainRegistry, sharedRequestsBuilderFactory, bulkRetriever)

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
        chainRegistry: ChainRegistry,
        multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi
    ) = RpcCalls(chainRegistry, multiChainRuntimeCallsApi)

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

    @Provides
    @ApplicationScope
    fun provideBlockLimitsRepository(
        @Named(LOCAL_STORAGE_SOURCE) localStorageSource: StorageDataSource,
    ): BlockLimitsRepository = RealBlockLimitsRepository(localStorageSource)

    @Provides
    @ApplicationScope
    fun provideExtrinsicSplitter(
        rpcCalls: RpcCalls,
        blockLimitsRepository: BlockLimitsRepository,
    ): ExtrinsicSplitter = RealExtrinsicSplitter(rpcCalls, blockLimitsRepository)

    @Provides
    @ApplicationScope
    fun provideStorageSharedRequestBuilderFactory(chainRegistry: ChainRegistry) = StorageSharedRequestsBuilderFactory(chainRegistry)

    @Provides
    @ApplicationScope
    fun provideMultiChainRuntimeCallsApi(chainRegistry: ChainRegistry): MultiChainRuntimeCallsApi = RealMultiChainRuntimeCallsApi(chainRegistry)

    @Provides
    @ApplicationScope
    fun provideGasPriceProviderFactory(
        chainRegistry: ChainRegistry
    ): GasPriceProviderFactory = RealGasPriceProviderFactory(chainRegistry)

    @Provides
    @ApplicationScope
    fun provideMultiLocationConverterFactory(chainRegistry: ChainRegistry): MultiLocationConverterFactory {
        return MultiLocationConverterFactory(chainRegistry)
    }

    @Provides
    @ApplicationScope
    fun provideExtrinsicWalk(
        chainRegistry: ChainRegistry,
    ): ExtrinsicWalk = RealExtrinsicWalk(chainRegistry)

    @Provides
    @ApplicationScope
    fun provideChainNodeRepository(
        chainDao: ChainDao,
    ): ChainNodeRepository = RealChainNodeRepository(chainDao)

    @Provides
    @ApplicationScope
    fun provideNodeConnectionFactory(
        socketServiceProvider: Provider<SocketService>,
        connectionSecrets: ConnectionSecrets
    ): NodeConnectionFactory {
        return NodeConnectionFactory(
            socketServiceProvider,
            connectionSecrets
        )
    }

    @Provides
    @ApplicationScope
    fun provideRemoteToDomainChainMapperFacade(gson: Gson): RemoteToDomainChainMapperFacade {
        return RemoteToDomainChainMapperFacade(gson)
    }

    @Provides
    @ApplicationScope
    fun providePreConfiguredChainsRepository(
        chainFetcher: ChainFetcher,
        chainMapperFacade: RemoteToDomainChainMapperFacade
    ): PreConfiguredChainsRepository {
        return RealPreConfiguredChainsRepository(
            chainFetcher,
            chainMapperFacade
        )
    }

    @Provides
    @ApplicationScope
    fun provideChainRepository(
        chainDao: ChainDao,
        gson: Gson
    ): ChainRepository {
        return RealChainRepository(
            chainDao,
            gson
        )
    }
}
