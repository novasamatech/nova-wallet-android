package io.novafoundation.nova.feature_staking_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.AccountStakingDao
import io.novafoundation.nova.core_db.dao.StakingRewardPeriodDao
import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.StakingApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.SubQueryValidatorSetFetcher
import io.novafoundation.nova.feature_staking_impl.data.repository.BagListRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.LocalBagListRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.ParasRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.PayoutRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealParasRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealSessionRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealStakingPeriodRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealStakingVersioningRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.SessionRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingPeriodRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRepositoryImpl
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingVersioningRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.AuraSession
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.BabeSession
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSessionRegistry
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.RealElectionsSessionRegistry
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.RealStakingRewardPeriodDataSource
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.StakingRewardPeriodDataSource
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.StakingRewardsDataSource
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.StakingStoriesDataSource
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.StakingStoriesDataSourceImpl
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.SubqueryStakingRewardsDataSource
import io.novafoundation.nova.feature_staking_impl.di.staking.DefaultBulkRetriever
import io.novafoundation.nova.feature_staking_impl.di.staking.PayoutsBulkRetriever
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.alerts.AlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.EraTimeCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.payout.PayoutInteractor
import io.novafoundation.nova.feature_staking_impl.domain.period.RealStakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.setup.SetupStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.bond.BondMoreInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.controller.ControllerInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.rebond.RebondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.rewardDestination.ChangeRewardDestinationInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validators.ValidatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.validators.current.CurrentValidatorsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validators.current.search.SearchCustomValidatorsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.common.hints.StakingHintsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationMixin
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationProvider
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.RealStakingDashboardPresentationMapper
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapper
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_wallet_api.di.common.AssetUseCaseModule
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

const val PAYOUTS_BULK_RETRIEVER_PAGE_SIZE = 500
const val DEFAULT_BULK_RETRIEVER_PAGE_SIZE = 1000

@Module(includes = [AssetUseCaseModule::class])
class StakingFeatureModule {

    @Provides
    @FeatureScope
    fun provideFeeLoaderMixin(
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        tokenUseCase: TokenUseCase,
    ): FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(tokenUseCase)

    @Provides
    @FeatureScope
    fun provideStakingSharedState() = StakingSharedState()

    @Provides
    @FeatureScope
    fun provideSelectableSharedState(stakingSharedState: StakingSharedState): SelectedAssetOptionSharedState<*> = stakingSharedState

    @Provides
    @FeatureScope
    fun provideStakingStoriesDataSource(): StakingStoriesDataSource = StakingStoriesDataSourceImpl()

    @Provides
    @FeatureScope
    fun provideStakingRewardsSubqueryDataSource(
        stakingApi: StakingApi,
        stakingTotalRewardDao: StakingTotalRewardDao,
    ): StakingRewardsDataSource = SubqueryStakingRewardsDataSource(
        stakingApi = stakingApi,
        stakingTotalRewardDao = stakingTotalRewardDao
    )

    @Provides
    @FeatureScope
    fun provideStakingRepository(
        accountStakingDao: AccountStakingDao,
        @Named(LOCAL_STORAGE_SOURCE) localStorageSource: StorageDataSource,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        stakingStoriesDataSource: StakingStoriesDataSource,
        walletConstants: WalletConstants,
        chainRegistry: ChainRegistry,
    ): StakingRepository = StakingRepositoryImpl(
        accountStakingDao = accountStakingDao,
        remoteStorage = remoteStorageSource,
        localStorage = localStorageSource,
        stakingStoriesDataSource = stakingStoriesDataSource,
        walletConstants = walletConstants,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideStakingSharedComputation(
        computationalCache: ComputationalCache,
        stakingRepository: StakingRepository,
        rewardCalculatorFactory: RewardCalculatorFactory,
        accountRepository: AccountRepository,
        bagListRepository: BagListRepository,
        totalIssuanceRepository: TotalIssuanceRepository,
        eraTimeCalculatorFactory: EraTimeCalculatorFactory
    ) = StakingSharedComputation(
        stakingRepository = stakingRepository,
        computationalCache = computationalCache,
        rewardCalculatorFactory = rewardCalculatorFactory,
        accountRepository = accountRepository,
        bagListRepository = bagListRepository,
        totalIssuanceRepository = totalIssuanceRepository,
        eraTimeCalculatorFactory = eraTimeCalculatorFactory
    )

    @Provides
    @FeatureScope
    fun provideBagListRepository(
        @Named(LOCAL_STORAGE_SOURCE) localStorageSource: StorageDataSource,
    ): BagListRepository = LocalBagListRepository(localStorageSource)

    @Provides
    @FeatureScope
    fun provideParasRepository(
        @Named(LOCAL_STORAGE_SOURCE) localStorageSource: StorageDataSource,
    ): ParasRepository = RealParasRepository(localStorageSource)

    @Provides
    @FeatureScope
    fun provideStakingInteractor(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        stakingRepository: StakingRepository,
        stakingRewardsRepository: StakingRewardsRepository,
        stakingConstantsRepository: StakingConstantsRepository,
        identityRepository: OnChainIdentityRepository,
        payoutRepository: PayoutRepository,
        stakingSharedState: StakingSharedState,
        assetUseCase: AssetUseCase,
        stakingSharedComputation: StakingSharedComputation,
    ) = StakingInteractor(
        walletRepository,
        accountRepository,
        stakingRepository,
        stakingRewardsRepository,
        stakingConstantsRepository,
        identityRepository,
        stakingSharedState,
        payoutRepository,
        assetUseCase,
        stakingSharedComputation,
    )

    @Provides
    @FeatureScope
    fun provideAuraConsensus(
        chainRegistry: ChainRegistry,
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource,
    ) = AuraSession(chainRegistry, storageDataSource)

    @Provides
    @FeatureScope
    fun provideBabeConsensus(
        chainRegistry: ChainRegistry,
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource,
    ) = BabeSession(storageDataSource, chainRegistry)

    @Provides
    @FeatureScope
    fun provideElectionsSessionRegistry(
        auraSession: AuraSession,
        babeSession: BabeSession
    ): ElectionsSessionRegistry = RealElectionsSessionRegistry(babeSession, auraSession)

    @Provides
    @FeatureScope
    fun provideSessionRepository(
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource,
    ): SessionRepository = RealSessionRepository(storageDataSource)

    @Provides
    @FeatureScope
    fun provideEraTimeCalculatorFactory(
        stakingRepository: StakingRepository,
        sessionRepository: SessionRepository,
        chainStateRepository: ChainStateRepository,
        electionsSessionRegistry: ElectionsSessionRegistry,
    ) = EraTimeCalculatorFactory(stakingRepository, sessionRepository, chainStateRepository, electionsSessionRegistry)

    @Provides
    @FeatureScope
    fun provideAlertsInteractor(
        stakingRepository: StakingRepository,
        stakingConstantsRepository: StakingConstantsRepository,
        walletRepository: WalletRepository,
        bagListRepository: BagListRepository,
        totalIssuanceRepository: TotalIssuanceRepository,
        stakingSharedComputation: StakingSharedComputation,
    ) = AlertsInteractor(
        stakingRepository,
        stakingConstantsRepository,
        walletRepository,
        stakingSharedComputation,
        bagListRepository,
        totalIssuanceRepository
    )

    @Provides
    @FeatureScope
    fun provideRewardCalculatorFactory(
        repository: StakingRepository,
        totalIssuanceRepository: TotalIssuanceRepository,
        stakingSharedComputation: dagger.Lazy<StakingSharedComputation>,
        parasRepository: ParasRepository,
    ) = RewardCalculatorFactory(repository, totalIssuanceRepository, stakingSharedComputation, parasRepository)

    @Provides
    @FeatureScope
    fun provideValidatorRecommendatorFactory(
        validatorProvider: ValidatorProvider,
        computationalCache: ComputationalCache,
        sharedState: StakingSharedState,
    ) = ValidatorRecommendatorFactory(validatorProvider, sharedState, computationalCache)

    @Provides
    @FeatureScope
    fun provideValidatorProvider(
        stakingRepository: StakingRepository,
        identityRepository: OnChainIdentityRepository,
        rewardCalculatorFactory: RewardCalculatorFactory,
        stakingConstantsRepository: StakingConstantsRepository,
        stakingSharedComputation: StakingSharedComputation
    ) = ValidatorProvider(
        stakingRepository,
        identityRepository,
        rewardCalculatorFactory,
        stakingConstantsRepository,
        stakingSharedComputation
    )

    @Provides
    @FeatureScope
    fun provideStakingConstantsRepository(
        chainRegistry: ChainRegistry,
    ) = StakingConstantsRepository(chainRegistry)

    @Provides
    @FeatureScope
    fun provideRecommendationSettingsProviderFactory(
        stakingConstantsRepository: StakingConstantsRepository,
        computationalCache: ComputationalCache,
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = RecommendationSettingsProviderFactory(
        computationalCache,
        stakingConstantsRepository,
        chainRegistry,
        sharedState
    )

    @Provides
    @FeatureScope
    fun provideSetupStakingInteractor(
        extrinsicService: ExtrinsicService,
        sharedState: StakingSharedState,
    ) = SetupStakingInteractor(extrinsicService, sharedState)

    @Provides
    @FeatureScope
    fun provideSetupStakingSharedState() = SetupStakingSharedState()

    @Provides
    fun provideRewardDestinationChooserMixin(
        resourceManager: ResourceManager,
        appLinksProvider: AppLinksProvider,
        stakingInteractor: StakingInteractor,
        iconGenerator: AddressIconGenerator,
        accountDisplayUseCase: AddressDisplayUseCase,
        sharedState: StakingSharedState,
    ): RewardDestinationMixin.Presentation = RewardDestinationProvider(
        resourceManager,
        stakingInteractor,
        iconGenerator,
        appLinksProvider,
        sharedState,
        accountDisplayUseCase
    )

    @Provides
    @FeatureScope
    fun provideStakingRewardsApi(networkApiCreator: NetworkApiCreator): StakingApi {
        return networkApiCreator.create(StakingApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideStakingRewardsRepository(
        rewardDataSource: StakingRewardsDataSource,
    ): StakingRewardsRepository {
        return StakingRewardsRepository(rewardDataSource)
    }

    @Provides
    @FeatureScope
    fun provideValidatorSetFetcher(
        stakingApi: StakingApi,
        stakingRepository: StakingRepository,
    ): SubQueryValidatorSetFetcher {
        return SubQueryValidatorSetFetcher(
            stakingApi,
            stakingRepository
        )
    }

    @Provides
    @FeatureScope
    @DefaultBulkRetriever
    fun provideDefaultBulkRetriever(): BulkRetriever {
        return BulkRetriever(DEFAULT_BULK_RETRIEVER_PAGE_SIZE)
    }

    @Provides
    @FeatureScope
    @PayoutsBulkRetriever
    fun providePayoutBulkRetriever(): BulkRetriever {
        return BulkRetriever(PAYOUTS_BULK_RETRIEVER_PAGE_SIZE)
    }

    @Provides
    @FeatureScope
    fun providePayoutRepository(
        stakingRepository: StakingRepository,
        validatorSetFetcher: SubQueryValidatorSetFetcher,
        @PayoutsBulkRetriever bulkRetriever: BulkRetriever,
        storageCache: StorageCache,
        chainRegistry: ChainRegistry,
    ): PayoutRepository {
        return PayoutRepository(stakingRepository, bulkRetriever, validatorSetFetcher, storageCache, chainRegistry)
    }

    @Provides
    @FeatureScope
    fun providePayoutInteractor(
        sharedState: StakingSharedState,
        extrinsicService: ExtrinsicService,
    ) = PayoutInteractor(sharedState, extrinsicService)

    @Provides
    @FeatureScope
    fun provideBondMoreInteractor(
        sharedState: StakingSharedState,
        extrinsicService: ExtrinsicService,
    ) = BondMoreInteractor(extrinsicService, sharedState)

    @Provides
    @FeatureScope
    fun provideRedeemInteractor(
        extrinsicService: ExtrinsicService,
        stakingRepository: StakingRepository,
    ) = RedeemInteractor(extrinsicService, stakingRepository)

    @Provides
    @FeatureScope
    fun provideRebondInteractor(
        sharedState: StakingSharedState,
        extrinsicService: ExtrinsicService,
    ) = RebondInteractor(extrinsicService, sharedState)

    @Provides
    @FeatureScope
    fun provideStakingVersioningRepository(chainRegistry: ChainRegistry): StakingVersioningRepository {
        return RealStakingVersioningRepository(chainRegistry)
    }

    @Provides
    @FeatureScope
    fun provideControllerInteractor(
        sharedState: StakingSharedState,
        extrinsicService: ExtrinsicService,
        stakingVersioningRepository: StakingVersioningRepository
    ) = ControllerInteractor(extrinsicService, sharedState, stakingVersioningRepository)

    @Provides
    @FeatureScope
    fun provideCurrentValidatorsInteractor(
        stakingRepository: StakingRepository,
        stakingConstantsRepository: StakingConstantsRepository,
        validatorProvider: ValidatorProvider,
        stahingSharedState: StakingSharedState,
        accountRepository: AccountRepository,
        stakingSharedComputation: StakingSharedComputation
    ) = CurrentValidatorsInteractor(
        stakingRepository,
        stakingConstantsRepository,
        validatorProvider,
        stahingSharedState,
        accountRepository,
        stakingSharedComputation,
    )

    @Provides
    @FeatureScope
    fun provideChangeRewardDestinationInteractor(
        extrinsicService: ExtrinsicService,
    ) = ChangeRewardDestinationInteractor(extrinsicService)

    @Provides
    @FeatureScope
    fun provideSearchCustomValidatorsInteractor(
        validatorProvider: ValidatorProvider,
        sharedState: StakingSharedState
    ) = SearchCustomValidatorsInteractor(validatorProvider, sharedState)

    @Provides
    @FeatureScope
    fun provideStakingHintsUseCase(
        resourceManager: ResourceManager,
        stakingInteractor: StakingInteractor
    ) = StakingHintsUseCase(resourceManager, stakingInteractor)

    @Provides
    @FeatureScope
    fun provideCompoundStatefullComponent(
        sharedState: StakingSharedState,
    ) = CompoundStakingComponentFactory(sharedState)

    @Provides
    @FeatureScope
    fun provideStakingRewardPeriodDataSource(
        stakingRewardPeriodDao: StakingRewardPeriodDao
    ): StakingRewardPeriodDataSource = RealStakingRewardPeriodDataSource(stakingRewardPeriodDao)

    @Provides
    @FeatureScope
    fun provideStakingPeriodRepository(
        dataSource: StakingRewardPeriodDataSource
    ): StakingPeriodRepository = RealStakingPeriodRepository(dataSource)

    @Provides
    @FeatureScope
    fun provideStakingRewardInteractor(
        stakingPeriodRepository: StakingPeriodRepository,
        accountRepository: AccountRepository
    ): StakingRewardPeriodInteractor = RealStakingRewardPeriodInteractor(
        stakingPeriodRepository,
        accountRepository
    )

    @Provides
    @FeatureScope
    fun provideStakingDashboardPresentationMapper(resourceManager: ResourceManager): StakingDashboardPresentationMapper {
        return RealStakingDashboardPresentationMapper(resourceManager)
    }
}
