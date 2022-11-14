package io.novafoundation.nova.runtime.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset.EvmAssetsSyncService
import io.novafoundation.nova.runtime.multiNetwork.asset.remote.AssetFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.ChainSyncService
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnectionFactory
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionPool
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.NodeAutobalancer
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.AutoBalanceStrategyProvider
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeFilesCache
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProviderPool
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeSubscriptionPool
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeSyncService
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.BaseTypeSynchronizer
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.TypesFetcher
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Provider

@Module
class ChainRegistryModule {

    @Provides
    @ApplicationScope
    fun provideChainFetcher(apiCreator: NetworkApiCreator) = apiCreator.create(ChainFetcher::class.java)

    @Provides
    @ApplicationScope
    fun provideChainSyncService(
        dao: ChainDao,
        chainAssetDao: ChainAssetDao,
        chainFetcher: ChainFetcher,
        gson: Gson
    ) = ChainSyncService(dao, chainFetcher, gson)

    @Provides
    @ApplicationScope
    fun provideAssetFetcher(apiCreator: NetworkApiCreator) = apiCreator.create(AssetFetcher::class.java)

    @Provides
    @ApplicationScope
    fun provideAssetSyncService(
        dao: ChainAssetDao,
        assetFetcher: AssetFetcher
    ) = EvmAssetsSyncService(dao, assetFetcher)

    @Provides
    @ApplicationScope
    fun provideRuntimeFactory(
        runtimeFilesCache: RuntimeFilesCache,
        chainDao: ChainDao,
        gson: Gson,
    ): RuntimeFactory {
        return RuntimeFactory(runtimeFilesCache, chainDao, gson)
    }

    @Provides
    @ApplicationScope
    fun provideRuntimeFilesCache(
        fileProvider: FileProvider,
    ) = RuntimeFilesCache(fileProvider)

    @Provides
    @ApplicationScope
    fun provideTypesFetcher(
        networkApiCreator: NetworkApiCreator,
    ) = networkApiCreator.create(TypesFetcher::class.java)

    @Provides
    @ApplicationScope
    fun provideRuntimeSyncService(
        typesFetcher: TypesFetcher,
        runtimeFilesCache: RuntimeFilesCache,
        chainDao: ChainDao,
    ) = RuntimeSyncService(typesFetcher, runtimeFilesCache, chainDao)

    @Provides
    @ApplicationScope
    fun provideBaseTypeSynchronizer(
        typesFetcher: TypesFetcher,
        runtimeFilesCache: RuntimeFilesCache,
    ) = BaseTypeSynchronizer(runtimeFilesCache, typesFetcher)

    @Provides
    @ApplicationScope
    fun provideRuntimeProviderPool(
        runtimeFactory: RuntimeFactory,
        runtimeSyncService: RuntimeSyncService,
        baseTypeSynchronizer: BaseTypeSynchronizer,
    ) = RuntimeProviderPool(runtimeFactory, runtimeSyncService, baseTypeSynchronizer)

    @Provides
    @ApplicationScope
    fun provideAutoBalanceProvider() = AutoBalanceStrategyProvider()

    @Provides
    @ApplicationScope
    fun provideNodeAutoBalancer(
        autoBalanceStrategyProvider: AutoBalanceStrategyProvider,
    ) = NodeAutobalancer(autoBalanceStrategyProvider)

    @Provides
    @ApplicationScope
    fun provideChainConnectionFactory(
        socketProvider: Provider<SocketService>,
        externalRequirementsFlow: MutableStateFlow<ChainConnection.ExternalRequirement>,
        nodeAutobalancer: NodeAutobalancer,
    ) = ChainConnectionFactory(
        externalRequirementsFlow,
        nodeAutobalancer,
        socketProvider
    )

    @Provides
    @ApplicationScope
    fun provideConnectionPool(chainConnectionFactory: ChainConnectionFactory) = ConnectionPool(chainConnectionFactory)

    @Provides
    @ApplicationScope
    fun provideRuntimeVersionSubscriptionPool(
        chainDao: ChainDao,
        runtimeSyncService: RuntimeSyncService,
    ) = RuntimeSubscriptionPool(chainDao, runtimeSyncService)

    @Provides
    @ApplicationScope
    fun provideExternalRequirementsFlow() = MutableStateFlow(ChainConnection.ExternalRequirement.FORBIDDEN)

    @Provides
    @ApplicationScope
    fun provideChainRegistry(
        runtimeProviderPool: RuntimeProviderPool,
        chainConnectionPool: ConnectionPool,
        runtimeSubscriptionPool: RuntimeSubscriptionPool,
        chainDao: ChainDao,
        chainSyncService: ChainSyncService,
        evmAssetsSyncService: EvmAssetsSyncService,
        baseTypeSynchronizer: BaseTypeSynchronizer,
        runtimeSyncService: RuntimeSyncService,
        gson: Gson
    ) = ChainRegistry(
        runtimeProviderPool,
        chainConnectionPool,
        runtimeSubscriptionPool,
        chainDao,
        chainSyncService,
        evmAssetsSyncService,
        baseTypeSynchronizer,
        runtimeSyncService,
        gson
    )
}
