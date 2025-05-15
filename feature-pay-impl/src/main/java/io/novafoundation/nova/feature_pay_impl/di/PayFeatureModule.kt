package io.novafoundation.nova.feature_pay_impl.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.NetworkStateService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_pay_impl.BuildConfig
import io.novafoundation.nova.feature_pay_impl.data.raise.auth.network.RaiseAuthInterceptor
import io.novafoundation.nova.feature_pay_impl.data.raise.auth.network.RaiseAuthRepository
import io.novafoundation.nova.feature_pay_impl.data.raise.auth.network.RaiseEndpoints
import io.novafoundation.nova.feature_pay_impl.data.raise.auth.network.RealRaiseAuthRepository
import io.novafoundation.nova.feature_pay_impl.data.raise.auth.storage.RaiseAuthStorage
import io.novafoundation.nova.feature_pay_impl.data.raise.auth.storage.RealRaiseAuthStorage
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.RealShopBrandsRepository
import io.novafoundation.nova.feature_pay_impl.data.ShopBrandsRepository
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.RaiseBrandsApi
import io.novafoundation.nova.feature_pay_impl.domain.ShopInteractor
import io.novafoundation.nova.feature_pay_impl.domain.brand.RealShopBrandsInteractor
import io.novafoundation.nova.feature_pay_impl.domain.brand.ShopBrandsInteractor
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.RaiseBrandsConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.RealRaiseBrandsConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.RaisePopularBrandsApi
import io.novafoundation.nova.feature_pay_impl.data.raise.cards.RealShopCardsRepository
import io.novafoundation.nova.feature_pay_impl.data.raise.cards.ShopCardsRepository
import io.novafoundation.nova.feature_pay_impl.data.raise.cards.network.RaiseCardsApi
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseAmountConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseCurrencyConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseDateConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RealRaiseAmountConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RealRaiseCurrencyConverter
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RealRaiseDateConverter
import io.novafoundation.nova.feature_pay_impl.domain.cards.RealShopCardsUseCase
import io.novafoundation.nova.feature_pay_impl.domain.cards.ShopCardsUseCase
import io.novafoundation.nova.feature_pay_impl.presentation.shop.common.BrandsPaginationMixinFactory
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

const val TIMEOUT_SECONDS = 20L

@Module
class PayFeatureModule {

    @Provides
    @FeatureScope
    fun provideRaiseAuthStorage(
        secretStoreV2: SecretStoreV2,
        encryptedPreferences: EncryptedPreferences,
        chainRegistry: ChainRegistry,
        gson: Gson,
    ): RaiseAuthStorage {
        return RealRaiseAuthStorage(
            secretStoreV2,
            encryptedPreferences,
            chainRegistry,
            gson
        )
    }

    @Provides
    @FeatureScope
    fun provideRiseBrandsApi(
        okHttpClient: OkHttpClient,
        networkApiCreator: NetworkApiCreator,
    ): RaiseBrandsApi {
        return networkApiCreator.create(RaiseBrandsApi::class.java, RaiseEndpoints.BASE_URL, okHttpClient)
    }

    @Provides
    @FeatureScope
    fun provideRaiseCardsApi(
        okHttpClient: OkHttpClient,
        networkApiCreator: NetworkApiCreator,
    ): RaiseCardsApi {
        return networkApiCreator.create(RaiseCardsApi::class.java, RaiseEndpoints.BASE_URL, okHttpClient)
    }

    @Provides
    @FeatureScope
    fun provideRaisePopularBrandsApi(
        networkApiCreator: NetworkApiCreator,
    ): RaisePopularBrandsApi {
        return networkApiCreator.create(RaisePopularBrandsApi::class.java)
    }

    @Provides
    @FeatureScope
    fun provideRaiseAuthRepository(
        storage: RaiseAuthStorage,
    ): RaiseAuthRepository {
        return RealRaiseAuthRepository(storage)
    }

    @Provides
    @FeatureScope
    fun provideOkHttpClient(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        raiseAuthRepository: RaiseAuthRepository,
        gson: Gson,
    ): OkHttpClient {
        // We don't need many connections for Rise - reduce consumed resources by this instance of OkhttpClient
        val reducedConnectionPool = ConnectionPool(maxIdleConnections = 1, keepAliveDuration = 5, TimeUnit.SECONDS)

        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(reducedConnectionPool)

        val interceptor = RaiseAuthInterceptor(chainRegistry, accountRepository, raiseAuthRepository, gson)
        builder.addInterceptor(interceptor)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }

        return builder.build().also {
            interceptor.client = it
        }
    }

    @Provides
    @FeatureScope
    fun provideRaiseAmountConverter(): RaiseAmountConverter = RealRaiseAmountConverter()

    @Provides
    @FeatureScope
    fun provideRaiseCurrencyConverter(currencyRepository: CurrencyRepository): RaiseCurrencyConverter = RealRaiseCurrencyConverter(currencyRepository)

    @Provides
    @FeatureScope
    fun provideRaiseBrandsConverter(raiseAmountConverter: RaiseAmountConverter): RaiseBrandsConverter = RealRaiseBrandsConverter(raiseAmountConverter)

    @Provides
    @FeatureScope
    fun provideRaiseDateConverter(): RaiseDateConverter = RealRaiseDateConverter()

    @Provides
    @FeatureScope
    fun provideShopInteractor(
        accountRepository: AccountRepository
    ) = ShopInteractor(accountRepository)

    @Provides
    @FeatureScope
    fun provideShopBrandsRepository(
        raiseBrandsApi: RaiseBrandsApi,
        raisePopularBrandsApi: RaisePopularBrandsApi,
        networkStateService: NetworkStateService,
        raiseBrandsConverter: RaiseBrandsConverter
    ): ShopBrandsRepository {
        return RealShopBrandsRepository(
            raiseBrandsApi,
            raisePopularBrandsApi,
            networkStateService,
            raiseBrandsConverter
        )
    }

    @Provides
    @FeatureScope
    fun provideShopCardsRepository(
        cardsApi: RaiseCardsApi,
        raiseAmountConverter: RaiseAmountConverter,
        raiseCurrencyConverter: RaiseCurrencyConverter,
        raiseBrandsConverter: RaiseBrandsConverter,
        raiseDateConverter: RaiseDateConverter,
        networkStateService: NetworkStateService,
    ): ShopCardsRepository {
        return RealShopCardsRepository(
            cardsApi,
            raiseAmountConverter,
            raiseCurrencyConverter,
            raiseBrandsConverter,
            raiseDateConverter,
            networkStateService
        )
    }


    @Provides
    @FeatureScope
    fun provideShopCardsUseCase(shopCardsRepository: ShopCardsRepository): ShopCardsUseCase {
        return RealShopCardsUseCase(shopCardsRepository)
    }

    @Provides
    @FeatureScope
    fun provideShopBrandsInteractorUseCase(
        brandsRepository: ShopBrandsRepository,
        shopBrandsUseCase: ShopCardsUseCase
    ): ShopBrandsInteractor {
        return RealShopBrandsInteractor(brandsRepository, shopBrandsUseCase)
    }

    @Provides
    @FeatureScope
    fun providePaginationMixinFactory(
        shopBrandsInteractor: ShopBrandsInteractor,
        resourceManager: ResourceManager
    ) = BrandsPaginationMixinFactory(shopBrandsInteractor, resourceManager)
}
