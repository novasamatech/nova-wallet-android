package io.novafoundation.nova.feature_staking_impl.di.staking.dashboard

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.config.GlobalConfigDataSource
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.StakingDashboardDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.domain.dashboard.StakingDashboardInteractor
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.RealStakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.RealStakingStatsDataSource
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.StakingStatsDataSource
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api.StakingStatsApi
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.RealStakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain.StakingDashboardUpdaterFactory
import io.novafoundation.nova.feature_staking_impl.data.dashboard.repository.RealStakingDashboardRepository
import io.novafoundation.nova.feature_staking_impl.data.dashboard.repository.RealTotalStakeChainComparatorProvider
import io.novafoundation.nova.feature_staking_impl.data.dashboard.repository.StakingDashboardRepository
import io.novafoundation.nova.feature_staking_impl.data.dashboard.repository.TotalStakeChainComparatorProvider
import com.google.gson.Gson
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.BuildConfig
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.BittensorDelegatesClient
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.StubSubtensorValidatorDataSource
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorPositionCache
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorSubnetFetcher
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorValidatorDataSource
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorValidatorProvider
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.TaoStatsValidatorDataSource
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorPositionFetcher
import io.novafoundation.nova.feature_staking_impl.domain.dashboard.RealStakingDashboardInteractor
import okhttp3.OkHttpClient
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class StakingDashboardModule {

    @Provides
    @FeatureScope
    fun provideStakingDashboardRepository(dao: StakingDashboardDao): StakingDashboardRepository = RealStakingDashboardRepository(dao)

    @Provides
    @FeatureScope
    fun provideStakingStatsApi(apiCreator: NetworkApiCreator): StakingStatsApi {
        return apiCreator.create(StakingStatsApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideStakingStatsDataSource(
        api: StakingStatsApi,
        globalConfigDataSource: GlobalConfigDataSource
    ): StakingStatsDataSource {
        return RealStakingStatsDataSource(
            api = api,
            globalConfigDataSource = globalConfigDataSource
        )
    }

    @Provides
    @FeatureScope
    fun provideStakingDashboardCache(dao: StakingDashboardDao): StakingDashboardCache = RealStakingDashboardCache(dao)

    @Provides
    @FeatureScope
    fun provideBittensorDelegatesClient(
        gson: Gson,
    ): BittensorDelegatesClient {
        // The shared app OkHttpClient isn't exposed via StakingFeatureDependencies
        // and adding it there would touch every existing staking-impl call site.
        // BittensorDelegatesClient hits a single static GitHub URL once per
        // session so a dedicated client is cheap and avoids the dependency tax.
        return BittensorDelegatesClient(OkHttpClient(), gson)
    }

    @Provides
    @FeatureScope
    fun provideSubtensorSubnetFetcher(
        gson: Gson,
    ): SubtensorSubnetFetcher = SubtensorSubnetFetcher(OkHttpClient(), gson)

    @Provides
    @FeatureScope
    fun provideSubtensorValidatorDataSource(
        gson: Gson,
    ): SubtensorValidatorDataSource {
        // Mirrors iOS `SubtensorStakeSetupViewFactory`: if a TaoStats key
        // is available in the build (read from local.properties or the
        // shared key file at build time — see feature-staking-impl/build.gradle),
        // use the TaoStats REST data source so APR + commission render in
        // the picker. Otherwise fall back to the empty stub so the picker
        // still works in identity-only mode.
        val key: String? = BuildConfig.TAOSTATS_API_KEY
        return if (!key.isNullOrBlank()) {
            TaoStatsValidatorDataSource(apiKey = key, httpClient = OkHttpClient(), gson = gson)
        } else {
            StubSubtensorValidatorDataSource()
        }
    }

    @Provides
    @FeatureScope
    fun provideSubtensorValidatorProvider(
        delegatesClient: BittensorDelegatesClient,
        dataSource: SubtensorValidatorDataSource,
        chainRegistry: ChainRegistry,
    ): SubtensorValidatorProvider {
        // Bittensor mainnet uses ss58 prefix 42. The validator picker only
        // ever runs on Bittensor, so a constant is fine — if a second TAO
        // chain ever appears the prefix can be plumbed through DI.
        return SubtensorValidatorProvider(
            delegatesClient = delegatesClient,
            dataSource = dataSource,
            identityAddressPrefix = 42,
        )
    }

    @Provides
    @FeatureScope
    fun provideSubtensorPositionFetcher(
        chainRegistry: ChainRegistry,
    ): SubtensorPositionFetcher = SubtensorPositionFetcher(chainRegistry)

    @Provides
    @FeatureScope
    fun provideSubtensorPositionCache(
        fetcher: SubtensorPositionFetcher,
    ): SubtensorPositionCache = SubtensorPositionCache(fetcher)

    @Provides
    @FeatureScope
    fun provideStakingDashboardUpdaterFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        stakingDashboardCache: StakingDashboardCache,
        nominationPoolBalanceRepository: NominationPoolStateRepository,
        poolAccountDerivation: PoolAccountDerivation,
        storageCache: StorageCache,
        balanceLocksRepository: BalanceLocksRepository,
        subtensorPositionCache: SubtensorPositionCache,
    ) = StakingDashboardUpdaterFactory(
        stakingDashboardCache = stakingDashboardCache,
        remoteStorageSource = remoteStorageSource,
        nominationPoolBalanceRepository = nominationPoolBalanceRepository,
        poolAccountDerivation = poolAccountDerivation,
        storageCache = storageCache,
        balanceLocksRepository = balanceLocksRepository,
        subtensorPositionCache = subtensorPositionCache,
    )

    @Provides
    @FeatureScope
    fun provideUpdateSystem(
        stakingStatsDataSource: StakingStatsDataSource,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        updaterFactory: StakingDashboardUpdaterFactory,
        sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
        stakingDashboardRepository: StakingDashboardRepository,
    ): StakingDashboardUpdateSystem = RealStakingDashboardUpdateSystem(
        stakingStatsDataSource = stakingStatsDataSource,
        accountRepository = accountRepository,
        chainRegistry = chainRegistry,
        updaterFactory = updaterFactory,
        sharedRequestsBuilderFactory = sharedRequestsBuilderFactory,
        stakingDashboardRepository = stakingDashboardRepository
    )

    @Provides
    @FeatureScope
    fun provideTotalStakeComparatorProvider(): TotalStakeChainComparatorProvider = RealTotalStakeChainComparatorProvider()

    @Provides
    @FeatureScope
    fun provideInteractor(
        dashboardRepository: StakingDashboardRepository,
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        stakingDashboardUpdateSystem: StakingDashboardUpdateSystem,
        dAppMetadataRepository: DAppMetadataRepository,
        walletRepository: WalletRepository,
        totalStakeChainComparatorProvider: TotalStakeChainComparatorProvider
    ): StakingDashboardInteractor = RealStakingDashboardInteractor(
        dashboardRepository = dashboardRepository,
        chainRegistry = chainRegistry,
        accountRepository = accountRepository,
        stakingDashboardSyncTracker = stakingDashboardUpdateSystem,
        walletRepository = walletRepository,
        dAppMetadataRepository = dAppMetadataRepository,
        totalStakeChainComparatorProvider = totalStakeChainComparatorProvider
    )
}
