package io.novafoundation.nova.runtime.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.BuildConfig
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.repository.AssetsIconModeService
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset.EvmAssetsSyncService
import io.novafoundation.nova.runtime.multiNetwork.asset.remote.AssetFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.ChainSyncService
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnectionFactory
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionPool
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.Web3ApiPool
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.NodeAutobalancer
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.NodeSelectionStrategyProvider
import io.novafoundation.nova.runtime.multiNetwork.connection.node.healthState.NodeHealthStateTesterFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.AsyncChainSyncDispatcher
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeCacheMigrator
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeFilesCache
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeMetadataFetcher
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProviderPool
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeSubscriptionPool
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeSyncService
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.BaseTypeSynchronizer
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.TypesFetcher
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.logging.HttpLoggingInterceptor
import org.web3j.protocol.http.HttpService
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
        chainAssetDao: ChainAssetDao,
        chainDao: ChainDao,
        assetFetcher: AssetFetcher,
        gson: Gson
    ) = EvmAssetsSyncService(chainDao, chainAssetDao, assetFetcher, gson)

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
    fun provideRuntimeMetadataFetcher(): RuntimeMetadataFetcher {
        return RuntimeMetadataFetcher()
    }

    @Provides
    @ApplicationScope
    fun provideRuntimeCacheMigrator(): RuntimeCacheMigrator {
        return RuntimeCacheMigrator()
    }

    @Provides
    @ApplicationScope
    fun provideRuntimeFilesCache(
        fileProvider: FileProvider,
        preferences: Preferences
    ) = RuntimeFilesCache(fileProvider, preferences)

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
        runtimeCacheMigrator: RuntimeCacheMigrator,
        runtimeMetadataFetcher: RuntimeMetadataFetcher
    ) = RuntimeSyncService(
        typesFetcher = typesFetcher,
        runtimeFilesCache = runtimeFilesCache,
        chainDao = chainDao,
        runtimeMetadataFetcher = runtimeMetadataFetcher,
        cacheMigrator = runtimeCacheMigrator,
        chainSyncDispatcher = AsyncChainSyncDispatcher()
    )

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
        runtimeFilesCache: RuntimeFilesCache,
        baseTypeSynchronizer: BaseTypeSynchronizer,
    ) = RuntimeProviderPool(runtimeFactory, runtimeSyncService, runtimeFilesCache, baseTypeSynchronizer)

    @Provides
    @ApplicationScope
    fun provideAutoBalanceProvider(
        connectionSecrets: ConnectionSecrets
    ) = NodeSelectionStrategyProvider(connectionSecrets)

    @Provides
    @ApplicationScope
    fun provideNodeAutoBalancer(
        nodeSelectionStrategyProvider: NodeSelectionStrategyProvider,
    ) = NodeAutobalancer(nodeSelectionStrategyProvider)

    @Provides
    @ApplicationScope
    fun provideConnectionSecrets(): ConnectionSecrets = ConnectionSecrets.default()

    @Provides
    @ApplicationScope
    fun provideNodeConnectionFactory(
        socketProvider: Provider<SocketService>,
        bulkRetriever: BulkRetriever,
        connectionSecrets: ConnectionSecrets,
        web3ApiFactory: Web3ApiFactory
    ) = NodeHealthStateTesterFactory(
        socketProvider,
        connectionSecrets,
        bulkRetriever,
        web3ApiFactory
    )

    @Provides
    @ApplicationScope
    fun provideChainConnectionFactory(
        socketProvider: Provider<SocketService>,
        externalRequirementsFlow: MutableStateFlow<ChainConnection.ExternalRequirement>,
        nodeAutobalancer: NodeAutobalancer,
    ) = ChainConnectionFactory(
        externalRequirementsFlow,
        nodeAutobalancer,
        socketProvider,
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
    fun provideWeb3ApiFactory(
        strategyProvider: NodeSelectionStrategyProvider,
    ): Web3ApiFactory {
        val builder = HttpService.getOkHttpClientBuilder()
        builder.interceptors().clear() // getOkHttpClientBuilder() adds logging interceptor which doesn't log into LogCat

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }

        val okHttpClient = builder.build()

        return Web3ApiFactory(strategyProvider = strategyProvider, httpClient = okHttpClient)
    }

    @Provides
    @ApplicationScope
    fun provideWeb3ApiPool(web3ApiFactory: Web3ApiFactory) = Web3ApiPool(web3ApiFactory)

    @Provides
    @ApplicationScope
    fun provideExternalRequirementsFlow() = MutableStateFlow(ChainConnection.ExternalRequirement.ALLOWED)

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
        web3ApiPool: Web3ApiPool,
        gson: Gson,
        assetsIconModeService: AssetsIconModeService
    ) = ChainRegistry(
        runtimeProviderPool,
        chainConnectionPool,
        runtimeSubscriptionPool,
        chainDao,
        chainSyncService,
        evmAssetsSyncService,
        baseTypeSynchronizer,
        runtimeSyncService,
        web3ApiPool,
        gson,
        assetsIconModeService
    )
}
