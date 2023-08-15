package io.novafoundation.nova.feature_crowdloan_impl.di.contributions

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.ContributionDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdateSystemFactory
import io.novafoundation.nova.feature_crowdloan_api.data.network.updater.ContributionsUpdaterFactory
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_api.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parallel.ParallelApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.updater.AssetBalanceScopeFactory
import io.novafoundation.nova.feature_crowdloan_impl.data.network.updater.RealContributionsUpdateSystemFactory
import io.novafoundation.nova.feature_crowdloan_impl.data.network.updater.RealContributionsUpdaterFactory
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source.LiquidAcalaContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source.ParallelContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.domain.contributions.RealContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.contributions.RealContributionsRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class ContributionsModule {

    @Provides
    @FeatureScope
    @IntoSet
    fun acalaLiquidSource(
        acalaApi: AcalaApi,
        parachainInfoRepository: ParachainInfoRepository
    ): ExternalContributionSource = LiquidAcalaContributionSource(acalaApi, parachainInfoRepository)

    @Provides
    @FeatureScope
    @IntoSet
    fun parallelSource(
        parallelApi: ParallelApi,
    ): ExternalContributionSource = ParallelContributionSource(parallelApi)

    @Provides
    @FeatureScope
    fun provideContributionsInteractor(
        crowdloanRepository: CrowdloanRepository,
        accountRepository: AccountRepository,
        crowdloanSharedState: CrowdloanSharedState,
        chainStateRepository: ChainStateRepository,
        contributionsRepository: ContributionsRepository,
        chainRegistry: ChainRegistry,
        contributionsUpdateSystemFactory: ContributionsUpdateSystemFactory
    ): ContributionsInteractor = RealContributionsInteractor(
        crowdloanRepository = crowdloanRepository,
        accountRepository = accountRepository,
        selectedAssetCrowdloanState = crowdloanSharedState,
        chainStateRepository = chainStateRepository,
        contributionsRepository = contributionsRepository,
        chainRegistry = chainRegistry,
        contributionsUpdateSystemFactory = contributionsUpdateSystemFactory
    )

    @Provides
    @FeatureScope
    fun provideContributionsRepository(
        externalContributionsSources: Set<@JvmSuppressWildcards ExternalContributionSource>,
        chainRegistry: ChainRegistry,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        contributionDao: ContributionDao
    ): ContributionsRepository {
        return RealContributionsRepository(
            externalContributionsSources.toList(),
            chainRegistry,
            remoteStorageSource,
            contributionDao
        )
    }

    @Provides
    @FeatureScope
    fun provideContributionsUpdaterFactory(
        contributionsRepository: ContributionsRepository,
        crowdloanRepository: CrowdloanRepository,
        contributionDao: ContributionDao
    ): ContributionsUpdaterFactory = RealContributionsUpdaterFactory(
        contributionsRepository,
        crowdloanRepository,
        contributionDao
    )

    @Provides
    @FeatureScope
    fun provideContributionUpdateSystemFactory(
        contributionsUpdaterFactory: ContributionsUpdaterFactory,
        chainRegistry: ChainRegistry,
        assetBalanceScopeFactory: AssetBalanceScopeFactory,
        storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    ): ContributionsUpdateSystemFactory = RealContributionsUpdateSystemFactory(
        chainRegistry = chainRegistry,
        contributionsUpdaterFactory = contributionsUpdaterFactory,
        assetBalanceScopeFactory = assetBalanceScopeFactory,
        storageSharedRequestsBuilderFactory = storageSharedRequestsBuilderFactory
    )

    @Provides
    @FeatureScope
    fun provideAssetBalanceScopeFactory(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository
    ) = AssetBalanceScopeFactory(walletRepository, accountRepository)
}
