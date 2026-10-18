package io.novafoundation.nova.feature_staking_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.BuildConfig
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesApi
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorEstimatesRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitRuntimeSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioLoader
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
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootClaimInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubmissionExecutor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetDetailsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetMetadataInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetQuoteInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetSafetyInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorChartAxisFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitFormatter
import io.novafoundation.nova.feature_wallet_api.data.network.priceApi.PRICE_API_BASE_URL
import io.novafoundation.nova.feature_wallet_api.data.repository.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
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
    fun provideBittensorExitFormatter(tokenFormatter: TokenFormatter, fiatFormatter: FiatFormatter) =
        BittensorExitFormatter(tokenFormatter, fiatFormatter)

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
        bittensorSubnetInteractor: BittensorSubnetInteractor
    ) = BittensorStakePreflightValidation(
        repository,
        accounts,
        wallets,
        balanceValidationSystem,
        bittensorSubnetInteractor
    )

    @Provides
    fun provideBittensorStakePreflightSystem(validation: BittensorStakePreflightValidation) = ValidationSystem(validation)

    @Provides
    fun provideBittensorValidatorInteractor(repository: BittensorSubnetValidatorRepository, chainRegistry: ChainRegistry) =
        BittensorValidatorInteractor(repository, chainRegistry)

    @Provides
    fun provideBittensorAmountFormatter(tokenFormatter: TokenFormatter, fiatFormatter: FiatFormatter, resources: ResourceManager) =
        BittensorAmountFormatter(tokenFormatter, fiatFormatter, resources)

    @Provides
    fun provideBittensorRetryController(resources: ResourceManager, logger: Logger) =
        BittensorRetryController(resources, logger)

    @Provides
    @FeatureScope
    fun provideBittensorValidatorSource(
        backend: BittensorValidatorBackend,
        validators: BittensorSubnetValidatorRepository,
        logger: Logger
    ): BittensorRootValidatorSource =
        RemoteBittensorRootValidatorSource(backend, validators, logger)

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
        swapSource: BittensorSwapSource
    ): BittensorSubnetRepository = RealBittensorSubnetRepository(runtimeCalls, configRepository, swapSource)

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
        prices: CoinPriceRepository,
        configRepository: BittensorConfigRepository
    ) = BittensorSubnetDetailsInteractor(markets, prices, configRepository)

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
    fun provideBittensorServiceFeeBeneficiary(): String = "5EAjYyJ8JaxyvHTSNuFp8fqjerTETjqhoM7MdZux2Ehzjxdq"

    @Provides
    fun provideBittensorSubnetStaking(
        chainRegistry: ChainRegistry,
        wallets: WalletRepository,
        validators: BittensorSubnetValidatorRepository,
        bittensorSubnetInteractor: BittensorSubnetInteractor,
        extrinsics: ExtrinsicService,
        @Named(SERVICE_FEE_BENEFICIARY) beneficiaryAddress: String,
        submission: BittensorSubmissionExecutor
    ) = BittensorSubnetStakingInteractor(
        chainRegistry,
        wallets,
        validators,
        bittensorSubnetInteractor,
        extrinsics,
        beneficiaryAddress,
        submission
    )

    @Provides
    @FeatureScope
    fun provideBittensorSwapSource(runtimeCallsApi: MultiChainRuntimeCallsApi): BittensorSwapSource =
        RealBittensorSwapSource(runtimeCallsApi)

    @Provides
    @FeatureScope
    fun provideBittensorPortfolioLoader(swapSource: BittensorSwapSource, logger: Logger) =
        BittensorPortfolioLoader(swapSource, logger)

    @Provides
    @FeatureScope
    fun provideBittensorPositionRepository(
        positionSource: BittensorPositionSource,
        portfolioLoader: BittensorPortfolioLoader,
        chainStateRepository: ChainStateRepository,
        computationalCache: ComputationalCache,
        logger: Logger
    ): BittensorPositionRepository = RealBittensorPositionRepository(
        positionSource,
        portfolioLoader,
        chainStateRepository,
        computationalCache,
        logger
    )

    @Provides
    @FeatureScope
    fun provideBittensorPortfolioRepository(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        walletRepository: WalletRepository,
        positionRepository: BittensorPositionRepository
    ): BittensorPortfolioRepository = RealBittensorPortfolioRepository(
        chainRegistry,
        accountRepository,
        walletRepository,
        positionRepository
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
        logger: Logger
    ): BittensorStakingRepository = RealBittensorStakingRepository(
        chainRegistry = chainRegistry,
        walletRepository = walletRepository,
        accountRepository = accountRepository,
        positionRepository = positionRepository,
        validatorSource = validatorSource,
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
    fun provideBittensorEstimates(
        @Attested apiCreator: NetworkApiCreator,
        infrastructureUrls: InfrastructureUrls,
        accounts: AccountRepository,
        wallets: WalletRepository,
        logger: Logger
    ): BittensorEstimatesRepository {
        val api = apiCreator.create(BittensorEstimatesApi::class.java)
        return BittensorEstimatesRepository(
            api,
            infrastructureUrls,
            12_000L,
            accounts,
            wallets,
            logger
        )
    }

    @Provides
    @FeatureScope
    fun provideBittensorSubmissionExecutor(extrinsics: ExtrinsicService) =
        BittensorSubmissionExecutor(extrinsics)

    @Provides
    fun provideBittensorRootClaimSource(
        @Named(REMOTE_STORAGE_SOURCE) storage: StorageDataSource,
        rpc: RpcCalls,
        chains: ChainRegistry
    ): BittensorRootClaimSource =
        RealBittensorRootClaimSource(storage, rpc, chains)

    @Provides
    fun provideBittensorRootClaim(
        chains: ChainRegistry,
        accounts: AccountRepository,
        wallets: WalletRepository,
        source: BittensorRootClaimSource,
        extrinsics: ExtrinsicService,
        submission: BittensorSubmissionExecutor,
        constants: WalletConstants,
        backend: BittensorValidatorBackend
    ) =
        BittensorRootClaimInteractor(
            chains,
            accounts,
            wallets,
            source,
            extrinsics,
            submission,
            constants,
            backend
        )

    @Provides
    fun provideBittensorExitSource(@Named(REMOTE_STORAGE_SOURCE) storage: StorageDataSource, rpc: RpcCalls, chains: ChainRegistry): BittensorExitRuntimeSource =
        RealBittensorExitRuntimeSource(storage, rpc, chains)

    @Provides
    fun provideBittensorSubnetMetadata(subnets: BittensorSubnetRepository) =
        BittensorSubnetMetadataInteractor(subnets)

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
        logger
    )
}
