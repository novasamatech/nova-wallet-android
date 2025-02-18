package io.novafoundation.nova.feature_wallet_impl.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.interfaces.FileCache
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.CoinPriceDao
import io.novafoundation.nova.core_db.dao.ExternalBalanceDao
import io.novafoundation.nova.core_db.dao.HoldsDao
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.dao.PhishingAddressDao
import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.data.cache.CoinPriceLocalDataSourceImpl
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcher
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.PaymentUpdaterFactory
import io.novafoundation.nova.feature_wallet_api.data.network.coingecko.PriceApi
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransactor
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainWeigher
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.ExternalBalanceRepository
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceLocalDataSource
import io.novafoundation.nova.feature_wallet_api.data.source.CoinPriceRemoteDataSource
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.RealArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.RealArbitraryTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.fee.FeeInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.ChainAssetRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CoinPriceRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.ProxyHaveEnoughFeeValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserProviderFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.provider.FeeLoaderProviderFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderV2Factory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.WssSubstrateSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.assets.history.realtime.substrate.SubstrateRealtimeOperationFetcherFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.balance.RealPaymentUpdaterFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.CrossChainConfigApi
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.RealCrossChainTransactor
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.RealCrossChainWeigher
import io.novafoundation.nova.feature_wallet_impl.data.network.phishing.PhishingApi
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.repository.CoinPriceRepositoryImpl
import io.novafoundation.nova.feature_wallet_impl.data.repository.RealBalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_impl.data.repository.RealBalanceLocksRepository
import io.novafoundation.nova.feature_wallet_impl.data.repository.RealChainAssetRepository
import io.novafoundation.nova.feature_wallet_impl.data.repository.RealCrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_impl.data.repository.RealExternalBalanceRepository
import io.novafoundation.nova.feature_wallet_impl.data.repository.RuntimeWalletConstants
import io.novafoundation.nova.feature_wallet_impl.data.repository.TokenRepositoryImpl
import io.novafoundation.nova.feature_wallet_impl.data.repository.WalletRepositoryImpl
import io.novafoundation.nova.feature_wallet_impl.data.source.RealCoinPriceDataSource
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.feature_wallet_impl.domain.RealCrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_impl.domain.fee.RealFeeInteractor
import io.novafoundation.nova.feature_wallet_impl.domain.validaiton.context.AssetValidationContextFactory
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.extrinsic.visitor.api.ExtrinsicWalk
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.XcmVersionDetector
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class WalletFeatureModule {

    @Provides
    @FeatureScope
    fun provideExternalBalancesRepository(
        externalBalanceDao: ExternalBalanceDao
    ): ExternalBalanceRepository {
        return RealExternalBalanceRepository(externalBalanceDao)
    }

    @Provides
    @FeatureScope
    fun provideSubQueryApi(networkApiCreator: NetworkApiCreator): SubQueryOperationsApi {
        return networkApiCreator.create(SubQueryOperationsApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideCoingeckoApi(networkApiCreator: NetworkApiCreator): PriceApi {
        return networkApiCreator.create(PriceApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideCoinPriceRemoteDataSource(
        priceApi: PriceApi,
        httpExceptionHandler: HttpExceptionHandler
    ): CoinPriceRemoteDataSource {
        return RealCoinPriceDataSource(priceApi, httpExceptionHandler)
    }

    @Provides
    @FeatureScope
    fun provideCoinPriceLocalDataSource(
        coinPriceDao: CoinPriceDao
    ): CoinPriceLocalDataSource {
        return CoinPriceLocalDataSourceImpl(coinPriceDao)
    }

    @Provides
    @FeatureScope
    fun provideAssetCache(
        tokenDao: TokenDao,
        assetDao: AssetDao,
        accountRepository: AccountRepository,
    ): AssetCache {
        return AssetCache(tokenDao, accountRepository, assetDao)
    }

    @Provides
    @FeatureScope
    fun providePhishingApi(networkApiCreator: NetworkApiCreator): PhishingApi {
        return networkApiCreator.create(PhishingApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideSubstrateSource(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
    ): SubstrateRemoteSource = WssSubstrateSource(
        remoteStorageSource,
    )

    @Provides
    @FeatureScope
    fun provideTokenRepository(
        tokenDao: TokenDao,
    ): TokenRepository = TokenRepositoryImpl(
        tokenDao
    )

    @Provides
    @FeatureScope
    fun provideCursorStorage(preferences: Preferences) = TransferCursorStorage(preferences)

    @Provides
    @FeatureScope
    fun provideWalletRepository(
        substrateSource: SubstrateRemoteSource,
        operationsDao: OperationDao,
        phishingApi: PhishingApi,
        phishingAddressDao: PhishingAddressDao,
        assetCache: AssetCache,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        coinPriceRemoteDataSource: CoinPriceRemoteDataSource
    ): WalletRepository = WalletRepositoryImpl(
        substrateSource,
        operationsDao,
        phishingApi,
        accountRepository,
        assetCache,
        phishingAddressDao,
        coinPriceRemoteDataSource,
        chainRegistry,
    )

    @Provides
    @FeatureScope
    fun providePaymentUpdaterFactory(
        operationDao: OperationDao,
        assetSourceRegistry: AssetSourceRegistry,
        accountUpdateScope: AccountUpdateScope,
        chainRegistry: ChainRegistry,
        assetCache: AssetCache
    ): PaymentUpdaterFactory = RealPaymentUpdaterFactory(
        operationDao,
        assetSourceRegistry,
        accountUpdateScope,
        chainRegistry,
        assetCache
    )

    @Provides
    @FeatureScope
    fun provideWalletConstants(
        chainRegistry: ChainRegistry,
    ): WalletConstants = RuntimeWalletConstants(chainRegistry)

    @Provides
    @FeatureScope
    fun provideAmountChooserFactory(
        resourceManager: ResourceManager,
        assetIconProvider: AssetIconProvider
    ): AmountChooserMixin.Factory = AmountChooserProviderFactory(resourceManager, assetIconProvider)

    @Provides
    @FeatureScope
    fun provideCustomFeeInteractor(
        feePaymentProviderRegistry: FeePaymentProviderRegistry,
        chainRegistry: ChainRegistry,
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        assetSourceRegistry: AssetSourceRegistry,
        customFeeCapabilityFacade: CustomFeeCapabilityFacade,
        tokenRepository: TokenRepository,
    ): FeeInteractor {
        return RealFeeInteractor(
            feePaymentProviderRegistry = feePaymentProviderRegistry,
            chainRegistry = chainRegistry,
            walletRepository = walletRepository,
            accountRepository = accountRepository,
            tokenRepository = tokenRepository,
            assetSourceRegistry = assetSourceRegistry,
            customFeeCapabilityFacade = customFeeCapabilityFacade,
        )
    }

    @Provides
    @FeatureScope
    fun provideFeeLoaderMixinFactory(
        resourceManager: ResourceManager,
        feeInteractor: FeeInteractor
    ): FeeLoaderMixin.Factory {
        return FeeLoaderProviderFactory(resourceManager, feeInteractor)
    }

    @Provides
    @FeatureScope
    fun provideFeeLoaderV2MixinFactory(
        chainRegistry: ChainRegistry,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        resourceManager: ResourceManager,
        interactor: FeeInteractor,
    ): FeeLoaderMixinV2.Factory {
        return FeeLoaderV2Factory(chainRegistry, actionAwaitableMixinFactory, resourceManager, interactor)
    }

    @Provides
    @FeatureScope
    fun provideCrossChainConfigApi(
        apiCreator: NetworkApiCreator
    ): CrossChainConfigApi = apiCreator.create(CrossChainConfigApi::class.java)

    @Provides
    @FeatureScope
    fun provideCrossChainRepository(
        api: CrossChainConfigApi,
        fileCache: FileCache,
        gson: Gson
    ): CrossChainTransfersRepository = RealCrossChainTransfersRepository(api, fileCache, gson)

    @Provides
    @FeatureScope
    fun provideCrossChainWeigher(
        @Named(REMOTE_STORAGE_SOURCE) storageDataSource: StorageDataSource,
        extrinsicService: ExtrinsicService,
        chainRegistry: ChainRegistry,
        xcmVersionDetector: XcmVersionDetector
    ): CrossChainWeigher = RealCrossChainWeigher(storageDataSource, extrinsicService, chainRegistry, xcmVersionDetector)

    @Provides
    @FeatureScope
    fun provideCrossChainTransactor(
        weigher: CrossChainWeigher,
        assetSourceRegistry: AssetSourceRegistry,
        phishingValidationFactory: PhishingValidationFactory,
        xcmVersionDetector: XcmVersionDetector,
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
        eventsRepository: EventsRepository,
        chainRegistry: ChainRegistry,
        chainStateRepository: ChainStateRepository
    ): CrossChainTransactor = RealCrossChainTransactor(
        weigher = weigher,
        assetSourceRegistry = assetSourceRegistry,
        phishingValidationFactory = phishingValidationFactory,
        xcmVersionDetector = xcmVersionDetector,
        enoughTotalToStayAboveEDValidationFactory = enoughTotalToStayAboveEDValidationFactory,
        eventsRepository = eventsRepository,
        chainStateRepository = chainStateRepository,
        chainRegistry = chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideBalanceLocksRepository(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        lockDao: LockDao
    ): BalanceLocksRepository {
        return RealBalanceLocksRepository(accountRepository, chainRegistry, lockDao)
    }

    @Provides
    @FeatureScope
    fun provideBalanceHoldsRepository(
        chainRegistry: ChainRegistry,
        holdsDao: HoldsDao
    ): BalanceHoldsRepository {
        return RealBalanceHoldsRepository(chainRegistry, holdsDao)
    }

    @Provides
    @FeatureScope
    fun provideChainAssetRepository(
        chainAssetDao: ChainAssetDao,
        gson: Gson
    ): ChainAssetRepository = RealChainAssetRepository(chainAssetDao, gson)

    @Provides
    @FeatureScope
    fun provideCoinPriceRepository(
        cacheDataSource: CoinPriceLocalDataSource,
        remoteDataSource: CoinPriceRemoteDataSource
    ): CoinPriceRepository = CoinPriceRepositoryImpl(cacheDataSource, remoteDataSource)

    @Provides
    @FeatureScope
    fun provideArbitraryTokenUseCase(
        coinPriceRepository: CoinPriceRepository,
        currencyRepository: CurrencyRepository
    ): ArbitraryTokenUseCase = RealArbitraryTokenUseCase(coinPriceRepository, currencyRepository)

    @Provides
    @FeatureScope
    fun provideArbitraryAssetUseCase(
        accountRepository: AccountRepository,
        walletRepository: WalletRepository,
        chainRegistry: ChainRegistry
    ): ArbitraryAssetUseCase = RealArbitraryAssetUseCase(accountRepository, walletRepository, chainRegistry)

    @Provides
    @FeatureScope
    fun provideEnoughTotalToStayAboveEDValidationFactory(
        assetSourceRegistry: AssetSourceRegistry
    ): EnoughTotalToStayAboveEDValidationFactory {
        return EnoughTotalToStayAboveEDValidationFactory(
            assetSourceRegistry
        )
    }

    @Provides
    @FeatureScope
    fun provideCrossChainTransfersUseCase(
        crossChainTransfersRepository: CrossChainTransfersRepository,
        walletRepository: WalletRepository,
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        computationalCache: ComputationalCache,
        crossChainWeigher: CrossChainWeigher,
        crossChainTransactor: CrossChainTransactor,
        parachainInfoRepository: ParachainInfoRepository,
    ): CrossChainTransfersUseCase {
        return RealCrossChainTransfersUseCase(
            crossChainTransfersRepository = crossChainTransfersRepository,
            walletRepository = walletRepository,
            chainRegistry = chainRegistry,
            accountRepository = accountRepository,
            computationalCache = computationalCache,
            crossChainWeigher = crossChainWeigher,
            crossChainTransactor = crossChainTransactor,
            parachainInfoRepository = parachainInfoRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideSubstrateRealtimeOperationFetcherFactory(
        multiLocationConverterFactory: MultiLocationConverterFactory,
        eventsRepository: EventsRepository,
        extrinsicWalk: ExtrinsicWalk,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter
    ): SubstrateRealtimeOperationFetcher.Factory {
        return SubstrateRealtimeOperationFetcherFactory(
            multiLocationConverterFactory = multiLocationConverterFactory,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            eventsRepository = eventsRepository,
            extrinsicWalk = extrinsicWalk
        )
    }

    @Provides
    @FeatureScope
    fun provideProxyHaveEnoughFeeValidationFactory(
        assetSourceRegistry: AssetSourceRegistry,
        walletRepository: WalletRepository,
        extrinsicService: ExtrinsicService,
    ) = ProxyHaveEnoughFeeValidationFactory(
        assetSourceRegistry,
        walletRepository,
        extrinsicService
    )

    @Provides
    @FeatureScope
    fun provideAssetsValidationContextFactory(
        arbitraryAssetUseCase: ArbitraryAssetUseCase,
        chainRegistry: ChainRegistry,
        assetSourceRegistry: AssetSourceRegistry,
    ): AssetsValidationContext.Factory {
        return AssetValidationContextFactory(arbitraryAssetUseCase, chainRegistry, assetSourceRegistry)
    }
}
