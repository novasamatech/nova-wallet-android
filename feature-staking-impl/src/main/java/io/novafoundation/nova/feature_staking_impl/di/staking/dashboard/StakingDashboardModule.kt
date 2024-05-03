package io.novafoundation.nova.feature_staking_impl.di.staking.dashboard

import dagger.Module
import dagger.Provides
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
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingGlobalConfigRepository
import io.novafoundation.nova.feature_staking_impl.domain.dashboard.RealStakingDashboardInteractor
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
        globalConfigRepository: StakingGlobalConfigRepository
    ): StakingStatsDataSource {
        return RealStakingStatsDataSource(
            api = api,
            stakingGlobalConfigRepository = globalConfigRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideStakingDashboardCache(dao: StakingDashboardDao): StakingDashboardCache = RealStakingDashboardCache(dao)

    @Provides
    @FeatureScope
    fun provideStakingDashboardUpdaterFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        stakingDashboardCache: StakingDashboardCache,
        nominationPoolBalanceRepository: NominationPoolStateRepository,
        poolAccountDerivation: PoolAccountDerivation,
        storageCache: StorageCache
    ) = StakingDashboardUpdaterFactory(stakingDashboardCache, remoteStorageSource, nominationPoolBalanceRepository, poolAccountDerivation, storageCache)

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
