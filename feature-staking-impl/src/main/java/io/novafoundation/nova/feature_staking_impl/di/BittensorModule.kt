package io.novafoundation.nova.feature_staking_impl.di

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPricesSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistoryApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorBackendClient
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetCatalogueSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioHistorySource
import io.novafoundation.nova.core_db.dao.ResponseCacheDao
import io.novafoundation.nova.feature_staking_impl.data.cache.ResponseCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExtrinsicOutcomeResolver
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
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorDashboardSnapshotSource
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
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorOperationHistorySource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRecommendedYieldInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorCostBasisInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorEstimatesMixin
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitRuntimeSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRuntimePriceSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioPriceSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetCatalogueApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetBrowseRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorPortfolioPriceSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootValidatorSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakePreflightRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetFavoritesRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetMarketsApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioFxSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorDelegateSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorDelegateSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorsApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorExitRuntimeSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorPositionRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorPositionRemoteSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionRemoteSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorRootClaimSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorStakePreflightRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorSwapSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RemoteBittensorConfigSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RemoteBittensorRootValidatorSource
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorClaimValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorExitValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorStakeValidationsModule
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPresetValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorBlockRequotes
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorImmediateStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubmissionExecutor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.RealBittensorExtrinsicOutcomeResolver
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
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
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import javax.inject.Named

private const val SERVICE_FEE_BENEFICIARY = "BittensorServiceFeeBeneficiary"

@Module(includes = [BittensorStakeValidationsModule::class, BittensorExitValidationsModule::class, BittensorClaimValidationsModule::class])
class BittensorModule {

    @Provides
    @FeatureScope
    @Named("BittensorCacheScope")
    fun provideBittensorCacheScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @FeatureScope
    fun provideResponseCache(
        dao: ResponseCacheDao,
        gson: Gson,
        @Named("BittensorCacheScope") scope: CoroutineScope,
        logger: Logger,
        interactionGate: AutomaticInteractionGate
    ) = ResponseCache(dao, gson, scope, logger, interactionGate.isInteractionAllowedFlow)

    @Provides
    @FeatureScope
    fun providePortfolioHistoryApi(@Attested apiCreator: NetworkApiCreator): BittensorPortfolioHistoryApi =
        apiCreator.create(BittensorPortfolioHistoryApi::class.java)

    @Provides
    @FeatureScope
    fun provideBittensorBackendClient(responses: ResponseCache, urls: InfrastructureUrls) = BittensorBackendClient(responses, urls)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetCatalogueSource(client: BittensorBackendClient, api: BittensorSubnetCatalogueApi) =
        BittensorSubnetCatalogueSource(client, api)

    @Provides
    @FeatureScope
    fun provideBittensorValidatorSource(client: BittensorBackendClient, api: BittensorValidatorsApi) = BittensorValidatorSource(client, api)

    @Provides
    @FeatureScope
    fun provideBittensorAlphaYieldSource(client: BittensorBackendClient, api: BittensorEstimatesApi) = BittensorAlphaYieldSource(client, api)

    @Provides
    @FeatureScope
    fun provideBittensorPortfolioFxSource(
        coinPriceRepository: CoinPriceRepository,
        cache: ResponseCache,
        computationalCache: ComputationalCache,
        logger: Logger
    ) = BittensorPortfolioFxSource(coinPriceRepository, cache, computationalCache, logger)

    @Provides
    @FeatureScope
    fun provideBittensorPortfolioHistorySource(client: BittensorBackendClient, api: BittensorPortfolioHistoryApi) =
        BittensorPortfolioHistorySource(client, api)

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
        validatorSource: BittensorValidatorSource,
        subnetCatalogue: BittensorSubnetCatalogueSource
    ) = BittensorValidatorInteractor(repository, chainRegistry, alphaYields, validatorSource, subnetCatalogue)

    @Provides
    fun provideBittensorAmountFormatter(tokenFormatter: TokenFormatter, fiatFormatter: FiatFormatter, resources: ResourceManager) =
        BittensorAmountFormatter(tokenFormatter, fiatFormatter, resources)

    @Provides
    fun provideBittensorRetryController(resources: ResourceManager, logger: Logger, automaticInteractionGate: AutomaticInteractionGate) =
        BittensorRetryController(resources, logger, automaticInteractionGate)

    @Provides
    fun provideBittensorRecommendedYieldInteractor(
        validatorSource: BittensorValidatorSource,
        alphaYieldRepository: BittensorAlphaYieldRepository,
        chainRegistry: ChainRegistry,
        logger: Logger
    ) = BittensorRecommendedYieldInteractor(validatorSource, alphaYieldRepository, chainRegistry, logger)

    @Provides
    @FeatureScope
    fun provideBittensorRootValidatorSource(
        validatorSource: BittensorValidatorSource,
        logger: Logger
    ): BittensorRootValidatorSource =
        RemoteBittensorRootValidatorSource(validatorSource, logger)

    @Provides
    fun provideBittensorConfigApi(apiCreator: NetworkApiCreator): BittensorConfigApi = apiCreator.create(BittensorConfigApi::class.java)

    @Provides
    @FeatureScope
    fun provideBittensorConfigSource(
        api: BittensorConfigApi,
        logger: Logger,
        @Named("BittensorCacheScope") scope: CoroutineScope,
        cache: ResponseCache
    ): BittensorConfigSource = RemoteBittensorConfigSource(api, BuildConfig.BITTENSOR_CONFIG_URL, logger, scope, cache.foregroundRefreshes)

    @Provides
    @FeatureScope
    fun provideBittensorPositionRemoteSource(runtimeCallsApi: MultiChainRuntimeCallsApi): BittensorPositionRemoteSource =
        RealBittensorPositionRemoteSource(runtimeCallsApi)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetSource(
        runtimeCalls: MultiChainRuntimeCallsApi,
        catalogueSource: BittensorSubnetCatalogueSource,
        computationalCache: ComputationalCache
    ) = BittensorSubnetSource(runtimeCalls, catalogueSource, computationalCache)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetRepository(
        subnetSource: BittensorSubnetSource,
        configSource: BittensorConfigSource,
        swapSource: BittensorSwapSource
    ): BittensorSubnetRepository = RealBittensorSubnetRepository(subnetSource, configSource, swapSource)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetBrowseRepository(
        catalogueSource: BittensorSubnetCatalogueSource,
        configSource: BittensorConfigSource
    ) = BittensorSubnetBrowseRepository(catalogueSource, configSource)

    @Provides
    @FeatureScope
    fun provideBittensorDashboardSnapshots(
        cache: ResponseCache,
        preferences: Preferences,
        gson: Gson,
        logger: Logger
    ) = BittensorDashboardSnapshotSource(cache, preferences, gson, logger)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetFavorites(accounts: AccountRepository, preferences: Preferences) =
        BittensorSubnetFavoritesRepository(accounts, preferences)

    @Provides
    fun provideBittensorChartAxisFormatterFactory(fiatFormatter: FiatFormatter, tokenFormatter: TokenFormatter) =
        BittensorChartAxisFormatter.Factory(fiatFormatter, tokenFormatter)

    @Provides
    fun provideBittensorSubnetQuotes(source: BittensorSwapSource, configSource: BittensorConfigSource) =
        BittensorSubnetQuoteInteractor(source, configSource)

    @Provides
    fun provideBittensorSubnetInteractor(
        repository: BittensorSubnetRepository,
        portfolioRepository: BittensorPortfolioRepository,
        bittensorSubnetQuoteInteractor: BittensorSubnetQuoteInteractor,
        favorites: BittensorSubnetFavoritesRepository,
        configSource: BittensorConfigSource
    ) = BittensorSubnetInteractor(
        repository,
        portfolioRepository,
        bittensorSubnetQuoteInteractor,
        favorites,
        configSource
    )

    @Provides
    fun provideBittensorSubnetValidators(
        validatorSource: BittensorValidatorSource,
        @Named(REMOTE_STORAGE_SOURCE) storage: StorageDataSource,
        rpc: RpcCalls
    ): BittensorSubnetValidatorRepository =
        RealBittensorSubnetValidatorRepository(validatorSource, storage, rpc)

    @Provides
    fun provideBittensorSubnetMarketsApi(apiCreator: NetworkApiCreator): BittensorSubnetMarketsApi =
        apiCreator.create(BittensorSubnetMarketsApi::class.java, PRICE_API_BASE_URL)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetMarkets(api: BittensorSubnetMarketsApi) = BittensorSubnetMarketSource(api)

    @Provides
    fun provideBittensorSubnetDetails(
        markets: BittensorSubnetMarketSource,
        prices: CoinPriceRepository
    ) = BittensorSubnetDetailsInteractor(markets, prices)

    @Provides
    @FeatureScope
    fun provideBittensorValidatorsApi(@Attested apiCreator: NetworkApiCreator): BittensorValidatorsApi =
        apiCreator.create(BittensorValidatorsApi::class.java)

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
        blockRequotes: BittensorBlockRequotes
    ) = BittensorSubnetStakingInteractor(
        chainRegistry,
        wallets,
        validators,
        bittensorSubnetInteractor,
        extrinsics,
        beneficiaryAddress,
        submission,
        blockRequotes
    )

    @Provides
    @FeatureScope
    fun provideBittensorSwapSource(runtimeCallsApi: MultiChainRuntimeCallsApi, rpcCalls: RpcCalls): BittensorSwapSource =
        RealBittensorSwapSource(runtimeCallsApi, rpcCalls)

    @Provides
    @FeatureScope
    fun provideBittensorSubnetCatalogueApi(@Attested apiCreator: NetworkApiCreator): BittensorSubnetCatalogueApi =
        apiCreator.create(BittensorSubnetCatalogueApi::class.java)

    @Provides
    @FeatureScope
    fun provideBittensorRuntimePriceSource(runtimeCalls: MultiChainRuntimeCallsApi) = BittensorRuntimePriceSource(runtimeCalls)

    @Provides
    @FeatureScope
    fun provideBittensorDelegateSource(runtimeCalls: MultiChainRuntimeCallsApi): BittensorDelegateSource = RealBittensorDelegateSource(runtimeCalls)

    @Provides
    @FeatureScope
    fun provideBittensorPortfolioPriceSource(
        catalogueSource: BittensorSubnetCatalogueSource,
        runtimePrices: BittensorRuntimePriceSource,
        logger: Logger
    ): BittensorPortfolioPriceSource = RealBittensorPortfolioPriceSource(catalogueSource, runtimePrices, logger)

    @Provides
    @FeatureScope
    fun provideBittensorPositionRepository(
        positionSource: BittensorPositionSource,
        pricesSource: BittensorPricesSource
    ): BittensorPositionRepository = RealBittensorPositionRepository(positionSource, pricesSource)

    @Provides
    @FeatureScope
    fun provideBittensorPositionSource(
        remoteSource: BittensorPositionRemoteSource,
        chainStateRepository: ChainStateRepository,
        computationalCache: ComputationalCache,
        logger: Logger,
        positionUpdates: BittensorPositionUpdates,
        cache: ResponseCache
    ) = BittensorPositionSource(remoteSource, chainStateRepository, computationalCache, logger, positionUpdates, cache)

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
        validatorSource: BittensorRootValidatorSource
    ): BittensorStakingRepository = RealBittensorStakingRepository(
        chainRegistry = chainRegistry,
        walletRepository = walletRepository,
        accountRepository = accountRepository,
        positionRepository = positionRepository,
        validatorSource = validatorSource
    )

    @Provides
    @FeatureScope
    fun provideBittensorStakingInteractor(
        repository: BittensorStakingRepository,
        presetValidatorInteractor: BittensorPresetValidatorInteractor,
        extrinsicService: ExtrinsicService,
        walletRepository: WalletRepository,
        validators: BittensorSubnetValidatorRepository,
        submission: BittensorSubmissionExecutor,
        logger: Logger
    ) = BittensorStakingInteractor(
        repository,
        presetValidatorInteractor,
        extrinsicService,
        walletRepository,
        validators,
        submission,
        logger
    )

    @Provides
    @FeatureScope
    fun provideBittensorEstimatesApi(@Attested apiCreator: NetworkApiCreator): BittensorEstimatesApi =
        apiCreator.create(BittensorEstimatesApi::class.java)

    @Provides
    @FeatureScope
    fun provideBittensorOperationHistorySource(
        client: BittensorBackendClient,
        api: BittensorEstimatesApi,
        portfolioHistory: BittensorPortfolioHistorySource,
        infrastructureUrls: InfrastructureUrls,
        computationalCache: ComputationalCache,
        preferences: Preferences
    ) = BittensorOperationHistorySource(client, api, portfolioHistory, infrastructureUrls, computationalCache, preferences, spacingMillis = 12_000L)

    @Provides
    @FeatureScope
    fun provideBittensorAlphaYieldRepository(
        yieldSource: BittensorAlphaYieldSource,
        infrastructureUrls: InfrastructureUrls
    ) = BittensorAlphaYieldRepository(yieldSource, infrastructureUrls)

    @Provides
    @FeatureScope
    fun provideBittensorCostBasisInteractor(
        historySource: BittensorOperationHistorySource,
        accounts: AccountRepository
    ) = BittensorCostBasisInteractor(historySource, accounts)

    @Provides
    fun provideBittensorEstimatesMixin(
        costBasisInteractor: BittensorCostBasisInteractor,
        alphaYieldRepository: BittensorAlphaYieldRepository,
        logger: Logger
    ) = BittensorEstimatesMixin(costBasisInteractor, alphaYieldRepository, logger)

    @Provides
    @FeatureScope
    fun provideBittensorOutcomeResolver(eventsRepository: EventsRepository, chainRegistry: ChainRegistry): BittensorExtrinsicOutcomeResolver =
        RealBittensorExtrinsicOutcomeResolver(eventsRepository, chainRegistry)

    @Provides
    @FeatureScope
    fun provideBittensorSubmissionExecutor(
        extrinsics: ExtrinsicService,
        accountRepository: AccountRepository,
        positionUpdates: BittensorPositionUpdates,
        outcomeResolver: BittensorExtrinsicOutcomeResolver,
        positions: BittensorPositionRepository,
        history: BittensorOperationHistorySource,
        logger: Logger
    ) = BittensorSubmissionExecutor(extrinsics, accountRepository, positionUpdates, positions, history, logger, outcomeResolver)

    @Provides
    @FeatureScope
    fun provideBittensorPricesSource(
        priceSource: BittensorPortfolioPriceSource,
        cache: ResponseCache,
        computationalCache: ComputationalCache
    ) = BittensorPricesSource(priceSource, cache, computationalCache)

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
    @FeatureScope
    fun provideBittensorDashboardDetailsSource(
        positions: BittensorPositionRepository,
        validators: BittensorValidatorInteractor,
        recommendedYields: BittensorRecommendedYieldInteractor,
        snapshots: BittensorDashboardSnapshotSource,
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
        config: BittensorConfigSource,
        subnetInteractor: BittensorSubnetInteractor,
        extrinsics: ExtrinsicService,
        submission: BittensorSubmissionExecutor,
        constants: WalletConstants,
        logger: Logger,
        immediateExecution: BittensorImmediateExecution,
        chainState: ChainStateRepository,
        blockRequotes: BittensorBlockRequotes,
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
        chainState,
        blockRequotes
    )
}
