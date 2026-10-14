package io.novafoundation.nova.feature_staking_impl.di

import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.announcements.AnnouncementsRepository
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.domain.announcements.RealStakingAnnouncementsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.announcements.StakingAnnouncementsUseCase
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core_db.dao.AccountStakingDao
import io.novafoundation.nova.core_db.dao.ExternalBalanceDao
import io.novafoundation.nova.core_db.dao.StakingRewardPeriodDao
import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.feature_account_api.data.externalAccounts.ExternalAccountsSyncService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.LocalIdentity
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_proxy_api.data.common.ProxyDepositCalculator
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.data.repository.ProxyConstantsRepository
import io.novafoundation.nova.feature_staking_api.data.network.blockhain.updaters.PooledBalanceUpdaterFactory
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display.PoolDisplayUseCase
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootValidatorSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketsApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_wallet_api.data.network.priceApi.PRICE_API_BASE_URL
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorSwapSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionUpdates
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorPositionSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RemoteBittensorRootValidatorSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEarnConfigApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetFavoritesRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorsApi
import io.novafoundation.nova.infrastructure.di.Attested
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetCommission
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetQuoteInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.BuildConfig
import io.novafoundation.nova.feature_staking_impl.data.dashboard.repository.StakingDashboardRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.StakingApi
import io.novafoundation.nova.feature_staking_impl.data.network.subquery.SubQueryValidatorSetFetcher
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.updater.RealPooledBalanceUpdaterFactory
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolDelegatedStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolMembersRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.repository.BagListRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.LocalBagListRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.ParasRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.PayoutRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealParasRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealSessionRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealStakingPeriodRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealStakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealStakingVersioningRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealVaraRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.SessionRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.RealUnstakingDurationRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.UnstakingDurationRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingPeriodRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRepositoryImpl
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingVersioningRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.VaraRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.AuraSession
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.BabeSession
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSessionRegistry
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.RealElectionsSessionRegistry
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.RealStakingRewardPeriodDataSource
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.StakingRewardPeriodDataSource
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward.DirectStakingRewardsDataSource
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward.PoolStakingRewardsDataSource
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward.RealStakingRewardsDataSourceRegistry
import io.novafoundation.nova.feature_staking_impl.data.repository.datasource.reward.StakingRewardsDataSourceRegistry
import io.novafoundation.nova.feature_staking_impl.data.validators.NovaValidatorsApi
import io.novafoundation.nova.feature_staking_impl.data.validators.RemoteValidatorsPreferencesSource
import io.novafoundation.nova.feature_staking_impl.data.validators.ValidatorsPreferencesSource
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureModule.BindsModule
import io.novafoundation.nova.feature_staking_impl.di.deeplinks.DeepLinkModule
import io.novafoundation.nova.feature_staking_impl.di.staking.DefaultBulkRetriever
import io.novafoundation.nova.feature_staking_impl.di.staking.PayoutsBulkRetriever
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingFlowState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.alerts.AlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.EraTimeCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.common.RealStakingHoldsMigrationUseCase
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingHoldsMigrationUseCase
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.era.StakingEraInteractorFactory
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.StakingTypesConflictValidationFactory
import io.novafoundation.nova.feature_staking_impl.domain.payout.PayoutInteractor
import io.novafoundation.nova.feature_staking_impl.domain.period.RealStakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.ValidatorRecommenderFactory
import io.novafoundation.nova.feature_staking_impl.domain.recommendations.settings.RecommendationSettingsProviderFactory
import io.novafoundation.nova.feature_staking_impl.domain.rewards.RewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.setup.ChangeValidatorsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.controller.ControllerInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.AddStakingProxyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.RealAddStakingProxyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list.RealStakingProxyListInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.list.StakingProxyListInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.remove.RealRemoveStakingProxyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.delegation.proxy.remove.RemoveStakingProxyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.rebond.RebondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.rewardDestination.ChangeRewardDestinationInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.RealStakingStartedDetectionService
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StakingStartedDetectionService
import io.novafoundation.nova.feature_staking_impl.domain.validators.ValidatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.validators.current.CurrentValidatorsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validators.current.search.SearchCustomValidatorsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.common.hints.StakingHintsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationMixin
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationProvider
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.StakingDashboardPresentationMapperFactory
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.display.RealPoolDisplayUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_wallet_api.di.common.AssetUseCaseModule
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.network.updaters.multiChain.DelegateToTimelineChainIdHolder
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

const val PAYOUTS_BULK_RETRIEVER_PAGE_SIZE = 500
const val DEFAULT_BULK_RETRIEVER_PAGE_SIZE = 1000

@Module(includes = [AssetUseCaseModule::class, DeepLinkModule::class, BindsModule::class])
class StakingFeatureModule {

    @Module
    interface BindsModule {

        @Binds
        fun bindHoldsMigrationUseCase(real: RealStakingHoldsMigrationUseCase): StakingHoldsMigrationUseCase
    }

    @Provides
    @FeatureScope
    fun provideTimelineDelegatingHolder(stakingSharedState: StakingSharedState) = DelegateToTimelineChainIdHolder(stakingSharedState)

    @Provides
    @FeatureScope
    fun provideStakingEraInteractorFactory(
        roundDurationEstimator: RoundDurationEstimator,
        stakingSharedComputation: StakingSharedComputation,
        unstakingDurationRepository: UnstakingDurationRepository,
        mythosSharedComputation: MythosSharedComputation,
        mythosStakingRepository: MythosStakingRepository,
    ) = StakingEraInteractorFactory(
        roundDurationEstimator = roundDurationEstimator,
        stakingSharedComputation = stakingSharedComputation,
        unstakingDurationRepository = unstakingDurationRepository,
        mythosSharedComputation = mythosSharedComputation,
        mythosStakingRepository = mythosStakingRepository
    )

    @Provides
    @FeatureScope
    fun provideFeeLoaderMixin(
        feeLoaderMixinFactory: FeeLoaderMixin.Factory,
        tokenUseCase: TokenUseCase,
    ): FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(tokenUseCase)

    @Provides
    @FeatureScope
    fun provideBittensorValidatorSource(apiCreator: NetworkApiCreator): BittensorRootValidatorSource =
        RemoteBittensorRootValidatorSource(apiCreator.create(BittensorEarnConfigApi::class.java), BuildConfig.BITTENSOR_EARN_CONFIG_URL)

    @Provides
    @FeatureScope
    fun provideBittensorPositionSource(runtimeCallsApi: MultiChainRuntimeCallsApi): BittensorPositionSource =
        RealBittensorPositionSource(runtimeCallsApi)

    @Provides
    @FeatureScope
    fun provideBittensorPositionUpdates() = BittensorPositionUpdates()

    @Provides
    @FeatureScope
    fun provideBittensorSubnetRepository(runtimeCalls: MultiChainRuntimeCallsApi): BittensorSubnetRepository =
        RealBittensorSubnetRepository(runtimeCalls)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetFavorites(accounts: AccountRepository, preferences: Preferences) =
        BittensorSubnetFavoritesRepository(accounts, preferences)

    @Provides
    fun provideBittensorSubnetCommission() = BittensorSubnetCommission(java.math.BigDecimal("0.003"))

    @Provides
    fun provideBittensorChartAxisFormatterFactory(fiatFormatter: FiatFormatter, tokenFormatter: TokenFormatter) =
        BittensorChartAxisFormatter.Factory(fiatFormatter, tokenFormatter)

    @Provides
    fun provideBittensorSubnetQuotes(source: BittensorSwapSource, commission: BittensorSubnetCommission) =
        BittensorSubnetQuoteInteractor(source, commission)

    @Provides
    fun provideBittensorSubnetInteractor(
        repository: BittensorSubnetRepository,
        portfolioRepository: BittensorPortfolioRepository,
        bittensorSubnetQuoteInteractor: BittensorSubnetQuoteInteractor,
        favorites: BittensorSubnetFavoritesRepository
    ) = BittensorSubnetInteractor(repository, portfolioRepository, bittensorSubnetQuoteInteractor, favorites)

    @Provides
    fun provideBittensorSubnetValidators(
        backend: BittensorValidatorBackend,
        @Named(REMOTE_STORAGE_SOURCE) storage: StorageDataSource,
        rpc: RpcCalls
    ): BittensorSubnetValidatorRepository =
        RealBittensorSubnetValidatorRepository(backend, storage, rpc)

    @Provides
    fun provideBittensorSubnetMarketsApi(apiCreator: NetworkApiCreator): BittensorSubnetMarketsApi =
        apiCreator.create(BittensorSubnetMarketsApi::class.java, PRICE_API_BASE_URL)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetMarkets(api: BittensorSubnetMarketsApi) = BittensorSubnetMarketRepository(api)

    @Provides
    fun provideBittensorSubnetDetails(
        markets: BittensorSubnetMarketRepository,
        prices: CoinPriceRepository,
        commission: BittensorSubnetCommission
    ) = BittensorSubnetDetailsInteractor(markets, prices, commission)

    @Provides
    @FeatureScope
    fun provideBittensorValidatorsApi(@Attested apiCreator: NetworkApiCreator): BittensorValidatorsApi =
        apiCreator.create(BittensorValidatorsApi::class.java)

    @Provides
    @FeatureScope
    fun provideBittensorValidatorBackend(
        api: BittensorValidatorsApi,
        infrastructureUrls: InfrastructureUrls
    ) = BittensorValidatorBackend(api, infrastructureUrls)

    @Provides
    fun provideBittensorSubnetStaking(
        chainRegistry: ChainRegistry,
        accounts: AccountRepository,
        wallets: WalletRepository,
        validators: BittensorSubnetValidatorRepository,
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        portfolio: BittensorPortfolioRepository,
        extrinsics: ExtrinsicService
    ) = BittensorSubnetStakingInteractor(
        chainRegistry,
        accounts,
        wallets,
        validators,
        bittensorSubnetInteractor,
        portfolio,
        extrinsics,
        "5EAjYyJ8JaxyvHTSNuFp8fqjerTETjqhoM7MdZux2Ehzjxdq"
    )

    @Provides
    @FeatureScope
    fun provideBittensorSwapSource(runtimeCallsApi: MultiChainRuntimeCallsApi): BittensorSwapSource =
        RealBittensorSwapSource(runtimeCallsApi)

    @Provides
    @FeatureScope
    fun provideBittensorPortfolioRepository(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        walletRepository: WalletRepository,
        positionSource: BittensorPositionSource,
        swapSource: BittensorSwapSource,
        positionUpdates: BittensorPositionUpdates,
        logger: Logger
    ): BittensorPortfolioRepository = RealBittensorPortfolioRepository(
        chainRegistry,
        accountRepository,
        walletRepository,
        positionSource,
        swapSource,
        positionUpdates,
        logger
    )

    @Provides
    @FeatureScope
    fun provideBittensorPortfolioInteractor(repository: BittensorPortfolioRepository) = BittensorPortfolioInteractor(repository)

    @Provides
    @FeatureScope
    fun provideBittensorStakingRepository(
        chainRegistry: ChainRegistry,
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        positionSource: BittensorPositionSource,
        validatorSource: BittensorRootValidatorSource,
        positionUpdates: BittensorPositionUpdates
    ): BittensorStakingRepository = RealBittensorStakingRepository(
        chainRegistry = chainRegistry,
        walletRepository = walletRepository,
        accountRepository = accountRepository,
        positionSource = positionSource,
        validatorSource = validatorSource,
        positionUpdates = positionUpdates
    )

    @Provides
    @FeatureScope
    fun provideBittensorStakingFlowState() = BittensorStakingFlowState()

    @Provides
    @FeatureScope
    fun provideBittensorStakingInteractor(
        repository: BittensorStakingRepository,
        extrinsicService: ExtrinsicService,
        flowState: BittensorStakingFlowState,
        accountRepository: AccountRepository,
        walletRepository: WalletRepository
    ) = BittensorStakingInteractor(repository, extrinsicService, flowState, accountRepository, walletRepository)

    @Provides
    @FeatureScope
    fun provideStakingSharedState() = StakingSharedState()

    @Provides
    @FeatureScope
    fun provideSelectableSharedState(stakingSharedState: StakingSharedState): SelectedAssetOptionSharedState<*> = stakingSharedState

    @Provides
    @FeatureScope
    fun provideStakingRepository(
        accountStakingDao: AccountStakingDao,
        @Named(LOCAL_STORAGE_SOURCE) localStorageSource: StorageDataSource,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        walletConstants: WalletConstants,
        chainRegistry: ChainRegistry,
        storageCache: StorageCache
    ): StakingRepository = StakingRepositoryImpl(
        accountStakingDao = accountStakingDao,
        remoteStorage = remoteStorageSource,
        localStorage = localStorageSource,
        walletConstants = walletConstants,
        chainRegistry = chainRegistry,
        storageCache = storageCache
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
        eraTimeCalculatorFactory: EraTimeCalculatorFactory,
        stakingConstantsRepository: StakingConstantsRepository
    ) = StakingSharedComputation(
        stakingRepository = stakingRepository,
        computationalCache = computationalCache,
        rewardCalculatorFactory = rewardCalculatorFactory,
        accountRepository = accountRepository,
        bagListRepository = bagListRepository,
        totalIssuanceRepository = totalIssuanceRepository,
        eraTimeCalculatorFactory = eraTimeCalculatorFactory,
        stakingConstantsRepository = stakingConstantsRepository
    )

    @Provides
    @FeatureScope
    fun provideBagListRepository(
        @Named(LOCAL_STORAGE_SOURCE) localStorageSource: StorageDataSource,
        chainRegistry: ChainRegistry
    ): BagListRepository = LocalBagListRepository(localStorageSource, chainRegistry)

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
        unstakingDurationRepository: UnstakingDurationRepository,
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
        unstakingDurationRepository,
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
    fun provideVaraRepository(chainRegistry: ChainRegistry): VaraRepository = RealVaraRepository(chainRegistry)

    @Provides
    @FeatureScope
    fun provideRewardCalculatorFactory(
        repository: StakingRepository,
        totalIssuanceRepository: TotalIssuanceRepository,
        stakingSharedComputation: dagger.Lazy<StakingSharedComputation>,
        parasRepository: ParasRepository,
        varaRepository: VaraRepository
    ) = RewardCalculatorFactory(
        stakingRepository = repository,
        totalIssuanceRepository = totalIssuanceRepository,
        shareStakingSharedComputation = stakingSharedComputation,
        parasRepository = parasRepository,
        varaRepository = varaRepository
    )

    @Provides
    @FeatureScope
    fun provideNovaValidatorsApi(apiCreator: NetworkApiCreator): NovaValidatorsApi {
        return apiCreator.create(NovaValidatorsApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideKnownNovaValidators(
        novaValidatorsApi: NovaValidatorsApi,
        chainRegistry: ChainRegistry
    ): ValidatorsPreferencesSource = RemoteValidatorsPreferencesSource(novaValidatorsApi, chainRegistry)

    @Provides
    @FeatureScope
    fun provideValidatorRecommendatorFactory(
        validatorProvider: ValidatorProvider,
        computationalCache: ComputationalCache,
        sharedState: StakingSharedState,
        validatorsPreferencesSource: ValidatorsPreferencesSource
    ) = ValidatorRecommenderFactory(validatorProvider, sharedState, computationalCache, validatorsPreferencesSource)

    @Provides
    @FeatureScope
    fun provideValidatorProvider(
        stakingRepository: StakingRepository,
        identityRepository: OnChainIdentityRepository,
        rewardCalculatorFactory: RewardCalculatorFactory,
        stakingConstantsRepository: StakingConstantsRepository,
        stakingSharedComputation: StakingSharedComputation,
        validatorsPreferencesSource: ValidatorsPreferencesSource
    ) = ValidatorProvider(
        stakingRepository = stakingRepository,
        identityRepository = identityRepository,
        rewardCalculatorFactory = rewardCalculatorFactory,
        stakingConstantsRepository = stakingConstantsRepository,
        stakingSharedComputation = stakingSharedComputation,
        validatorsPreferencesSource = validatorsPreferencesSource
    )

    @Provides
    @FeatureScope
    fun provideStakingConstantsRepository(
        chainRegistry: ChainRegistry,
        runtimeCallsApi: MultiChainRuntimeCallsApi
    ) = StakingConstantsRepository(chainRegistry, runtimeCallsApi)

    @Provides
    @FeatureScope
    fun provideUnstakingDurationRepository(
        chainRegistry: ChainRegistry,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        stakingConstantsRepository: StakingConstantsRepository,
    ): UnstakingDurationRepository = RealUnstakingDurationRepository(
        chainRegistry = chainRegistry,
        remoteStorageSource = remoteStorageSource,
        stakingConstantsRepository = stakingConstantsRepository
    )

    @Provides
    @FeatureScope
    fun provideRecommendationSettingsProviderFactory(
        computationalCache: ComputationalCache,
        sharedState: StakingSharedState,
        chainRegistry: ChainRegistry,
    ) = RecommendationSettingsProviderFactory(
        computationalCache,
        chainRegistry,
        sharedState
    )

    @Provides
    @FeatureScope
    fun provideChangeValidatorsInteractor(
        extrinsicService: ExtrinsicService,
        sharedState: StakingSharedState,
    ) = ChangeValidatorsInteractor(extrinsicService, sharedState)

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
    fun provideDirectStakingRewardsDataSource(
        stakingApi: StakingApi,
        stakingTotalRewardDao: StakingTotalRewardDao,
    ) = DirectStakingRewardsDataSource(
        stakingApi = stakingApi,
        stakingTotalRewardDao = stakingTotalRewardDao
    )

    @Provides
    @FeatureScope
    fun providePoolStakingRewardsDataSource(
        stakingApi: StakingApi,
        stakingTotalRewardDao: StakingTotalRewardDao,
    ) = PoolStakingRewardsDataSource(
        stakingApi = stakingApi,
        stakingTotalRewardDao = stakingTotalRewardDao
    )

    @Provides
    @FeatureScope
    fun provideStakingRewardsDataSourceRegistry(
        directStakingRewardsDataSource: DirectStakingRewardsDataSource,
        poolStakingRewardsDataSource: PoolStakingRewardsDataSource
    ): StakingRewardsDataSourceRegistry = RealStakingRewardsDataSourceRegistry(
        directStakingRewardsDataSource = directStakingRewardsDataSource,
        poolStakingRewardsDataSource = poolStakingRewardsDataSource
    )

    @Provides
    @FeatureScope
    fun provideStakingRewardsRepository(
        rewardsDataSourceRegistry: StakingRewardsDataSourceRegistry
    ): StakingRewardsRepository {
        return RealStakingRewardsRepository(rewardsDataSourceRegistry)
    }

    @Provides
    @FeatureScope
    fun provideValidatorSetFetcher(
        stakingApi: StakingApi,
    ): SubQueryValidatorSetFetcher {
        return SubQueryValidatorSetFetcher(stakingApi)
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
        chainRegistry: ChainRegistry,
        rpcCalls: RpcCalls,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource
    ): PayoutRepository {
        return PayoutRepository(stakingRepository, validatorSetFetcher, chainRegistry, remoteStorageSource, rpcCalls)
    }

    @Provides
    @FeatureScope
    fun providePayoutInteractor(
        sharedState: StakingSharedState,
        extrinsicService: ExtrinsicService,
    ) = PayoutInteractor(sharedState, extrinsicService)

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
    fun provideStakingDashboardPresentationMapper(
        resourceManager: ResourceManager,
        assetIconProvider: AssetIconProvider
    ): StakingDashboardPresentationMapperFactory {
        return StakingDashboardPresentationMapperFactory(resourceManager, assetIconProvider)
    }

    @Provides
    @FeatureScope
    fun providePooledBalanceUpdaterFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        poolAccountDerivation: PoolAccountDerivation,
        externalBalanceDao: ExternalBalanceDao,
        scope: AccountUpdateScope,
    ): PooledBalanceUpdaterFactory {
        return RealPooledBalanceUpdaterFactory(
            remoteStorageSource = remoteStorageSource,
            poolAccountDerivation = poolAccountDerivation,
            externalBalanceDao = externalBalanceDao,
            scope = scope
        )
    }

    @Provides
    @FeatureScope
    fun providePoolDisplayUseCase(
        poolDisplayFormatter: PoolDisplayFormatter,
        poolAccountDerivation: PoolAccountDerivation,
        poolStateRepository: NominationPoolStateRepository,
    ): PoolDisplayUseCase = RealPoolDisplayUseCase(
        poolDisplayFormatter = poolDisplayFormatter,
        poolAccountDerivation = poolAccountDerivation,
        poolStateRepository = poolStateRepository
    )

    @Provides
    @FeatureScope
    fun provideStakingStartedDetectionService(
        stakingDashboardRepository: StakingDashboardRepository,
        computationalCache: ComputationalCache,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
    ): StakingStartedDetectionService = RealStakingStartedDetectionService(
        stakingDashboardRepository = stakingDashboardRepository,
        computationalCache = computationalCache,
        accountRepository = accountRepository,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideAddProxyRepository(
        extrinsicService: ExtrinsicService,
        proxyDepositCalculator: ProxyDepositCalculator,
        getProxyRepository: GetProxyRepository,
        proxyConstantsRepository: ProxyConstantsRepository,
        externalAccountsSyncService: ExternalAccountsSyncService,
        extrinsicNavigationWrapper: ExtrinsicNavigationWrapper
    ): AddStakingProxyInteractor {
        return RealAddStakingProxyInteractor(
            extrinsicService,
            proxyDepositCalculator,
            getProxyRepository,
            proxyConstantsRepository,
            externalAccountsSyncService,
            extrinsicNavigationWrapper = extrinsicNavigationWrapper
        )
    }

    @Provides
    @FeatureScope
    fun provideStakingProxyListInteractor(
        getProxyRepository: GetProxyRepository,
        @LocalIdentity identityProvider: IdentityProvider
    ): StakingProxyListInteractor = RealStakingProxyListInteractor(
        getProxyRepository,
        identityProvider
    )

    @Provides
    @FeatureScope
    fun removeStakingProxyInteractor(
        extrinsicService: ExtrinsicService,
        externalAccountsSyncService: ExternalAccountsSyncService,
    ): RemoveStakingProxyInteractor = RealRemoveStakingProxyInteractor(
        extrinsicService = extrinsicService,
        externalAccountsSyncService = externalAccountsSyncService
    )

    @Provides
    @FeatureScope
    fun provideStakingAnnouncementsUseCase(
        announcementsRepository: AnnouncementsRepository
    ): StakingAnnouncementsUseCase = RealStakingAnnouncementsUseCase(announcementsRepository)

    @Provides
    @FeatureScope
    fun provideStakingConflictsValidationFactory(
        stakingRepository: StakingRepository,
        delegatedStakeRepository: NominationPoolDelegatedStakeRepository,
        nominationPoolStakingRepository: NominationPoolMembersRepository
    ): StakingTypesConflictValidationFactory {
        return StakingTypesConflictValidationFactory(
            stakingRepository = stakingRepository,
            delegatedStakeRepository = delegatedStakeRepository,
            nominationPoolStakingRepository = nominationPoolStakingRepository
        )
    }
}
