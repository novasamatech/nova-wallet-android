package io.novafoundation.nova.feature_wallet_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.dao.PhishingAddressDao
import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_api.data.cache.AssetCache
import io.novafoundation.nova.feature_wallet_api.di.Wallet
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.BuyTokenRegistry
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserProviderFactory
import io.novafoundation.nova.feature_wallet_impl.BuildConfig
import io.novafoundation.nova.feature_wallet_impl.data.buyToken.MoonPayProvider
import io.novafoundation.nova.feature_wallet_impl.data.buyToken.RampProvider
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.SubstrateRemoteSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.WssSubstrateSource
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.BalancesUpdateSystem
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.PaymentUpdaterFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.coingecko.CoingeckoApi
import io.novafoundation.nova.feature_wallet_impl.data.network.phishing.PhishingApi
import io.novafoundation.nova.feature_wallet_impl.data.network.subquery.SubQueryOperationsApi
import io.novafoundation.nova.feature_wallet_impl.data.repository.RuntimeWalletConstants
import io.novafoundation.nova.feature_wallet_impl.data.repository.TokenRepositoryImpl
import io.novafoundation.nova.feature_wallet_impl.data.repository.WalletRepositoryImpl
import io.novafoundation.nova.feature_wallet_impl.data.storage.TransferCursorStorage
import io.novafoundation.nova.feature_wallet_impl.domain.WalletInteractorImpl
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.assetActions.buy.BuyMixin
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.assetActions.buy.BuyMixinProvider
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferValidityChecks
import io.novafoundation.nova.feature_wallet_impl.presentation.send.TransferValidityChecksProvider
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.filter.HistoryFiltersProvider
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class WalletFeatureModule {

    @Provides
    @FeatureScope
    fun provideSubQueryApi(networkApiCreator: NetworkApiCreator): SubQueryOperationsApi {
        return networkApiCreator.create(SubQueryOperationsApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideCoingeckoApi(networkApiCreator: NetworkApiCreator): CoingeckoApi {
        return networkApiCreator.create(CoingeckoApi::class.java)
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
    fun provideHistoryFiltersProvider() = HistoryFiltersProvider()

    @Provides
    @FeatureScope
    fun provideSubstrateSource(
        rpcCalls: RpcCalls,
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        extrinsicService: ExtrinsicService,
    ): SubstrateRemoteSource = WssSubstrateSource(
        rpcCalls,
        remoteStorageSource,
        extrinsicService
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
        subQueryOperationsApi: SubQueryOperationsApi,
        httpExceptionHandler: HttpExceptionHandler,
        phishingApi: PhishingApi,
        phishingAddressDao: PhishingAddressDao,
        walletConstants: WalletConstants,
        assetCache: AssetCache,
        coingeckoApi: CoingeckoApi,
        accountRepository: AccountRepository,
        cursorStorage: TransferCursorStorage,
        chainRegistry: ChainRegistry,
        tokenDao: TokenDao,
    ): WalletRepository = WalletRepositoryImpl(
        substrateSource,
        operationsDao,
        subQueryOperationsApi,
        httpExceptionHandler,
        phishingApi,
        accountRepository,
        assetCache,
        walletConstants,
        phishingAddressDao,
        cursorStorage,
        coingeckoApi,
        chainRegistry,
        tokenDao
    )

    @Provides
    @FeatureScope
    fun provideWalletInteractor(
        walletRepository: WalletRepository,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        fileProvider: FileProvider,
    ): WalletInteractor = WalletInteractorImpl(
        walletRepository,
        accountRepository,
        chainRegistry,
        fileProvider
    )

    @Provides
    @FeatureScope
    fun provideBuyTokenIntegration(): BuyTokenRegistry {
        return BuyTokenRegistry(
            availableProviders = listOf(
                RampProvider(host = BuildConfig.RAMP_HOST, apiToken = BuildConfig.RAMP_TOKEN),
                MoonPayProvider(host = BuildConfig.MOONPAY_HOST, publicKey = BuildConfig.MOONPAY_PUBLIC_KEY, privateKey = BuildConfig.MOONPAY_PRIVATE_KEY)
            )
        )
    }

    @Provides
    fun provideBuyMixin(
        buyTokenRegistry: BuyTokenRegistry,
    ): BuyMixin.Presentation = BuyMixinProvider(buyTokenRegistry)

    @Provides
    @FeatureScope
    fun provideTransferChecks(): TransferValidityChecks.Presentation = TransferValidityChecksProvider()

    @Provides
    @FeatureScope
    fun providePaymentUpdaterFactory(
        remoteSource: SubstrateRemoteSource,
        assetCache: AssetCache,
        operationDao: OperationDao,
        accountUpdateScope: AccountUpdateScope,
        chainRegistry: ChainRegistry,
    ) = PaymentUpdaterFactory(
        remoteSource,
        assetCache,
        operationDao,
        chainRegistry,
        accountUpdateScope
    )

    @Provides
    @Wallet
    @FeatureScope
    fun provideFeatureUpdaters(
        chainRegistry: ChainRegistry,
        paymentUpdaterFactory: PaymentUpdaterFactory,
        accountUpdateScope: AccountUpdateScope,
    ): UpdateSystem = BalancesUpdateSystem(
        chainRegistry,
        paymentUpdaterFactory,
        accountUpdateScope,
    )

    @Provides
    @FeatureScope
    fun provideWalletConstants(
        chainRegistry: ChainRegistry,
    ): WalletConstants = RuntimeWalletConstants(chainRegistry)

    @Provides
    @FeatureScope
    fun provideAmountChooserFactory(): AmountChooserMixin.Factory = AmountChooserProviderFactory()
}
