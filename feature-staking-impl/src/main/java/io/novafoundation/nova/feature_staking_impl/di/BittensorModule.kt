package io.novafoundation.nova.feature_staking_impl.di

import com.google.gson.Gson
import android.os.SystemClock
import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.feature_staking_impl.di.staking.DefaultBulkRetriever
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.core_db.dao.ExternalBalanceDao
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_staking_api.data.network.blockhain.updaters.BittensorBalanceUpdaterFactory
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorBalanceUpdaterFactory
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorClaimClock
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorDashboardSnapshotRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorDashboardSnapshotRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPendingClaims
import io.novafoundation.nova.common.utils.formatting.baseDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.DurationFormatter
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorImmediateExecution
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.BuildConfig
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesRemoteSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorOperationHistoryRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRecommendedYieldInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorCostBasisInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitRuntimeSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioLoader
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRuntimePriceSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioPriceRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetCatalogueApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetCatalogueRemoteSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorPortfolioPriceRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootValidatorSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakePreflightRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetFavoritesRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketsApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorDelegateSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorDelegateSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorsApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorExitRuntimeSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorPositionRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorPositionSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorRootClaimSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorStakePreflightRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorSwapSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RemoteBittensorConfigRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RemoteBittensorRootValidatorSource
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorClaimValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorExitValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorStakeValidationsModule
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPresetValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorImmediateStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubmissionExecutor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionUpdates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetMetadataInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetQuoteInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetSafetyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.dashboard.BittensorDashboardDetailsSource
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_wallet_api.data.network.priceApi.PRICE_API_BASE_URL
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.BittensorServiceFeeRecipients
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.infrastructure.di.Attested
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import javax.inject.Named

private const val SERVICE_FEE_BENEFICIARY = "BittensorServiceFeeBeneficiary"

@Module(includes = [BittensorStakeValidationsModule::class, BittensorExitValidationsModule::class, BittensorClaimValidationsModule::class])
class BittensorModule {

    @Provides
    fun provideClaimClock(): BittensorClaimClock = BittensorClaimClock(SystemClock::elapsedRealtime)

    @Provides
    @FeatureScope
    fun providePendingClaims(clock: BittensorClaimClock) = BittensorPendingClaims(clock)

    @Provides
    @Named("BittensorWithdrawalDuration")
    fun provideWithdrawalDuration(contextManager: ContextManager): DurationFormatter =
        baseDurationFormatter(contextManager.getApplicationContext())

    @Provides
    @FeatureScope
    fun provideBittensorBalanceUpdaterFactory(
        scope: AccountUpdateScope,
        positionRepository: BittensorPositionRepository,
        externalBalanceDao: ExternalBalanceDao
    ): BittensorBalanceUpdaterFactory {
        return RealBittensorBalanceUpdaterFactory(scope, positionRepository, externalBalanceDao)
    }

    @Provides
    fun provideBittensorSubnetSafetyInteractor(rpcCalls: RpcCalls, chainStateRepository: ChainStateRepository) =
        BittensorSubnetSafetyInteractor(rpcCalls, chainStateRepository)

    @Provides
    fun provideBittensorStakePreflightRepository(
        @Named(REMOTE_STORAGE_SOURCE) storage: StorageDataSource,
        rpc: RpcCalls
    ): BittensorStakePreflightRepository = RealBittensorStakePreflightRepository(storage, rpc)

    @Provides
    fun provideBittensorStakePreflightValidation(
        repository: BittensorStakePreflightRepository,
        accounts: AccountRepository,
        wallets: WalletRepository,
        balanceValidationSystem: BittensorStakeValidationSystem,
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        validatorRepository: BittensorSubnetValidatorRepository,
        immediateExecution: BittensorImmediateExecution,
        immediateValidation: BittensorImmediateStakeValidation
    ) = BittensorStakePreflightValidation(
        repository,
        accounts,
        wallets,
        balanceValidationSystem,
        bittensorSubnetInteractor,
        validatorRepository,
        immediateExecution,
        immediateValidation
    )

    @Provides
    fun provideBittensorStakePreflightSystem(validation: BittensorStakePreflightValidation) = ValidationSystem(validation)

    @Provides
    fun provideBittensorImmediateStakeValidation() = BittensorImmediateStakeValidation()

    @Provides
    fun provideBittensorValidatorInteractor(
        repository: BittensorSubnetValidatorRepository,
        chainRegistry: ChainRegistry,
        alphaYields: BittensorAlphaYieldRepository,
        backend: BittensorValidatorBackend,
        subnetCatalogue: BittensorSubnetCatalogueRemoteSource
    ) = BittensorValidatorInteractor(repository, chainRegistry, alphaYields, backend, subnetCatalogue)

    @Provides
    fun provideBittensorAmountFormatter(tokenFormatter: TokenFormatter, fiatFormatter: FiatFormatter, resources: ResourceManager) =
        BittensorAmountFormatter(tokenFormatter, fiatFormatter, resources)

    @Provides
    fun provideBittensorRetryController(resources: ResourceManager, logger: Logger) =
        BittensorRetryController(resources, logger)

    @Provides
    fun provideBittensorRecommendedYieldInteractor(
        validatorBackend: BittensorValidatorBackend,
        alphaYieldRepository: BittensorAlphaYieldRepository,
        chainRegistry: ChainRegistry,
        logger: Logger
    ) = BittensorRecommendedYieldInteractor(validatorBackend, alphaYieldRepository, chainRegistry, logger)

    @Provides
    @FeatureScope
    fun provideBittensorValidatorSource(
        backend: BittensorValidatorBackend,
        logger: Logger
    ): BittensorRootValidatorSource =
        RemoteBittensorRootValidatorSource(backend, logger)

    @Provides
    fun provideBittensorConfigApi(apiCreator: NetworkApiCreator): BittensorConfigApi = apiCreator.create(BittensorConfigApi::class.java)

    @Provides
    @FeatureScope
    fun provideBittensorConfigRepository(api: BittensorConfigApi, logger: Logger): BittensorConfigRepository =
        RemoteBittensorConfigRepository(api, BuildConfig.BITTENSOR_CONFIG_URL, logger)

    @Provides
    @FeatureScope
    fun provideBittensorPositionSource(runtimeCallsApi: MultiChainRuntimeCallsApi): BittensorPositionSource =
        RealBittensorPositionSource(runtimeCallsApi)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetRepository(
        runtimeCalls: MultiChainRuntimeCallsApi,
        configRepository: BittensorConfigRepository,
        swapSource: BittensorSwapSource,
        catalogueSource: BittensorSubnetCatalogueRemoteSource
    ): BittensorSubnetRepository = RealBittensorSubnetRepository(runtimeCalls, configRepository, swapSource, catalogueSource)

    @Provides
    @FeatureScope
    fun provideBittensorDashboardSnapshots(preferences: Preferences, gson: Gson): BittensorDashboardSnapshotRepository =
        RealBittensorDashboardSnapshotRepository(preferences, gson)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetFavorites(accounts: AccountRepository, preferences: Preferences) =
        BittensorSubnetFavoritesRepository(accounts, preferences)

    @Provides
    fun provideBittensorChartAxisFormatterFactory(fiatFormatter: FiatFormatter, tokenFormatter: TokenFormatter) =
        BittensorChartAxisFormatter.Factory(fiatFormatter, tokenFormatter)

    @Provides
    fun provideBittensorSubnetQuotes(source: BittensorSwapSource, configRepository: BittensorConfigRepository) =
        BittensorSubnetQuoteInteractor(source, configRepository)

    @Provides
    fun provideBittensorSubnetInteractor(
        repository: BittensorSubnetRepository,
        portfolioRepository: BittensorPortfolioRepository,
        bittensorSubnetQuoteInteractor: BittensorSubnetQuoteInteractor,
        favorites: BittensorSubnetFavoritesRepository,
        configRepository: BittensorConfigRepository
    ) = BittensorSubnetInteractor(
        repository,
        portfolioRepository,
        bittensorSubnetQuoteInteractor,
        favorites,
        configRepository
    )

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
        prices: CoinPriceRepository
    ) = BittensorSubnetDetailsInteractor(markets, prices)

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
    @Named(SERVICE_FEE_BENEFICIARY)
    fun provideBittensorServiceFeeBeneficiary(): String = BittensorServiceFeeRecipients.CURRENT_ADDRESS

    @Provides
    fun provideBittensorSubnetStaking(
        chainRegistry: ChainRegistry,
        wallets: WalletRepository,
        validators: BittensorSubnetValidatorRepository,
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        extrinsics: ExtrinsicService,
        @Named(SERVICE_FEE_BENEFICIARY) beneficiaryAddress: String,
        submission: BittensorSubmissionExecutor,
        costBasis: BittensorCostBasisInteractor
    ) = BittensorSubnetStakingInteractor(
        chainRegistry,
        wallets,
        validators,
        bittensorSubnetInteractor,
        extrinsics,
        beneficiaryAddress,
        submission,
        costBasis
    )

    @Provides
    @FeatureScope
    fun provideBittensorSwapSource(runtimeCallsApi: MultiChainRuntimeCallsApi, rpcCalls: RpcCalls): BittensorSwapSource =
        RealBittensorSwapSource(runtimeCallsApi, rpcCalls)

    @Provides
    @FeatureScope
    fun provideBittensorPortfolioLoader(priceRepository: BittensorPortfolioPriceRepository, logger: Logger) =
        BittensorPortfolioLoader(priceRepository, logger)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetCatalogueApi(@Attested apiCreator: NetworkApiCreator): BittensorSubnetCatalogueApi =
        apiCreator.create(BittensorSubnetCatalogueApi::class.java)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetCatalogueRemoteSource(api: BittensorSubnetCatalogueApi, infrastructureUrls: InfrastructureUrls) =
        BittensorSubnetCatalogueRemoteSource(api, infrastructureUrls)

    @Provides
    @FeatureScope
    fun provideBittensorRuntimePriceSource(runtimeCalls: MultiChainRuntimeCallsApi) = BittensorRuntimePriceSource(runtimeCalls)

    @Provides
    @FeatureScope
    fun provideBittensorDelegateSource(runtimeCalls: MultiChainRuntimeCallsApi): BittensorDelegateSource = RealBittensorDelegateSource(runtimeCalls)

    @Provides
    @FeatureScope
    fun provideBittensorPortfolioPriceRepository(
        catalogueSource: BittensorSubnetCatalogueRemoteSource,
        runtimePrices: BittensorRuntimePriceSource,
        logger: Logger
    ): BittensorPortfolioPriceRepository = RealBittensorPortfolioPriceRepository(catalogueSource, runtimePrices, logger)

    @Provides
    @FeatureScope
    fun provideBittensorPositionRepository(
        positionSource: BittensorPositionSource,
        portfolioLoader: BittensorPortfolioLoader,
        chainStateRepository: ChainStateRepository,
        computationalCache: ComputationalCache,
        logger: Logger,
        positionUpdates: BittensorPositionUpdates
    ): BittensorPositionRepository = RealBittensorPositionRepository(
        positionSource,
        portfolioLoader,
        chainStateRepository,
        computationalCache,
        logger,
        positionUpdates
    )

    @Provides
    @FeatureScope
    fun provideBittensorPortfolioRepository(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        walletRepository: WalletRepository,
        positionRepository: BittensorPositionRepository,
        signerProvider: SignerProvider
    ): BittensorPortfolioRepository = RealBittensorPortfolioRepository(
        chainRegistry,
        accountRepository,
        walletRepository,
        positionRepository,
        signerProvider
    )

    @Provides
    @FeatureScope
    fun provideBittensorPortfolioInteractor(repository: BittensorPortfolioRepository, subnetRepository: BittensorSubnetRepository) =
        BittensorPortfolioInteractor(repository, subnetRepository)

    @Provides
    @FeatureScope
    fun provideBittensorStakingRepository(
        chainRegistry: ChainRegistry,
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        positionRepository: BittensorPositionRepository,
        validatorSource: BittensorRootValidatorSource,
        presetValidatorInteractor: BittensorPresetValidatorInteractor,
        logger: Logger
    ): BittensorStakingRepository = RealBittensorStakingRepository(
        chainRegistry = chainRegistry,
        walletRepository = walletRepository,
        accountRepository = accountRepository,
        positionRepository = positionRepository,
        validatorSource = validatorSource,
        presetValidatorInteractor = presetValidatorInteractor,
        logger = logger
    )

    @Provides
    @FeatureScope
    fun provideBittensorStakingInteractor(
        repository: BittensorStakingRepository,
        extrinsicService: ExtrinsicService,
        walletRepository: WalletRepository,
        validators: BittensorSubnetValidatorRepository,
        submission: BittensorSubmissionExecutor
    ) = BittensorStakingInteractor(
        repository,
        extrinsicService,
        walletRepository,
        validators,
        submission
    )

    @Provides
    @FeatureScope
    fun provideBittensorEstimatesApi(@Attested apiCreator: NetworkApiCreator): BittensorEstimatesApi =
        apiCreator.create(BittensorEstimatesApi::class.java)

    @Provides
    @FeatureScope
    fun provideBittensorEstimatesSource(api: BittensorEstimatesApi): BittensorEstimatesRemoteSource =
        BittensorEstimatesRemoteSource(api, spacingMillis = 12_000L)

    @Provides
    @FeatureScope
    fun provideBittensorHistoryRepository(
        remoteSource: BittensorEstimatesRemoteSource,
        infrastructureUrls: InfrastructureUrls,
        computationalCache: ComputationalCache,
        preferences: Preferences
    ) = BittensorOperationHistoryRepository(remoteSource, infrastructureUrls, computationalCache, preferences)

    @Provides
    @FeatureScope
    fun provideBittensorAlphaYieldRepository(
        remoteSource: BittensorEstimatesRemoteSource,
        infrastructureUrls: InfrastructureUrls
    ) = BittensorAlphaYieldRepository(remoteSource, infrastructureUrls)

    @Provides
    @FeatureScope
    fun provideBittensorCostBasisInteractor(
        historyRepository: BittensorOperationHistoryRepository,
        accounts: AccountRepository,
        logger: Logger
    ) = BittensorCostBasisInteractor(historyRepository, accounts, logger)

    @Provides
    fun provideBittensorEstimatesMixin(
        costBasisInteractor: BittensorCostBasisInteractor,
        alphaYieldRepository: BittensorAlphaYieldRepository,
        logger: Logger
    ) = BittensorEstimatesMixin(costBasisInteractor, alphaYieldRepository, logger)

    @Provides
    @FeatureScope
    fun provideBittensorSubmissionExecutor(extrinsics: ExtrinsicService, positionUpdates: BittensorPositionUpdates) =
        BittensorSubmissionExecutor(extrinsics, positionUpdates)

    @Provides
    @FeatureScope
    fun provideBittensorPositionUpdates() = BittensorPositionUpdates()

    @Provides
    fun provideBittensorRootClaimSource(
        @Named(REMOTE_STORAGE_SOURCE) storage: StorageDataSource,
        rpc: RpcCalls,
        chains: ChainRegistry,
        chainStateRepository: ChainStateRepository,
        computationalCache: ComputationalCache,
        logger: Logger,
        clock: BittensorClaimClock
    ): BittensorRootClaimSource =
        RealBittensorRootClaimSource(storage, rpc, chains, chainStateRepository, computationalCache, logger, clock)

    @Provides
    fun provideBittensorImmediateExecution(signerProvider: SignerProvider) =
        BittensorImmediateExecution(signerProvider)

    @Provides
    fun provideBittensorRootClaim(
        chains: ChainRegistry,
        accounts: AccountRepository,
        wallets: WalletRepository,
        source: BittensorRootClaimSource,
        extrinsics: ExtrinsicService,
        submission: BittensorSubmissionExecutor,
        constants: WalletConstants,
        immediateExecution: BittensorImmediateExecution,
        pendingClaims: BittensorPendingClaims,
        positionRepository: BittensorPositionRepository,
        rootValidatorSource: BittensorRootValidatorSource
    ) =
        BittensorRootClaimInteractor(
            chains,
            accounts,
            wallets,
            source,
            extrinsics,
            submission,
            constants,
            immediateExecution,
            pendingClaims,
            positionRepository,
            rootValidatorSource
        )

    @Provides
    fun provideBittensorExitSource(
        @Named(REMOTE_STORAGE_SOURCE) storage: StorageDataSource,
        rpc: RpcCalls,
        chains: ChainRegistry,
        @DefaultBulkRetriever bulkRetriever: BulkRetriever
    ): BittensorExitRuntimeSource = RealBittensorExitRuntimeSource(storage, rpc, chains, bulkRetriever)

    @Provides
    fun provideBittensorSubnetMetadata(subnets: BittensorSubnetRepository) =
        BittensorSubnetMetadataInteractor(subnets)

    @Provides
    fun provideBittensorDashboardDetailsSource(
        positions: BittensorPositionRepository,
        validators: BittensorValidatorInteractor,
        recommendedYields: BittensorRecommendedYieldInteractor,
        snapshots: BittensorDashboardSnapshotRepository,
        logger: Logger
    ) = BittensorDashboardDetailsSource(
        positions,
        validators,
        recommendedYields,
        snapshots,
        logger
    )

    @Provides
    fun provideBittensorExit(
        chains: ChainRegistry,
        accounts: AccountRepository,
        wallets: WalletRepository,
        source: BittensorExitRuntimeSource,
        subnets: BittensorSubnetMetadataInteractor,
        swaps: BittensorSwapSource,
        config: BittensorConfigRepository,
        subnetInteractor: BittensorSubnetInteractor,
        extrinsics: ExtrinsicService,
        submission: BittensorSubmissionExecutor,
        constants: WalletConstants,
        logger: Logger,
        immediateExecution: BittensorImmediateExecution,
        costBasis: BittensorCostBasisInteractor,
        chainState: ChainStateRepository,
        @Named(SERVICE_FEE_BENEFICIARY) beneficiaryAddress: String
    ) = BittensorExitInteractor(
        chains,
        accounts,
        wallets,
        source,
        subnets,
        swaps,
        config,
        subnetInteractor.slippage,
        extrinsics,
        submission,
        constants,
        beneficiaryAddress,
        logger,
        immediateExecution,
        costBasis,
        chainState
    )
}
