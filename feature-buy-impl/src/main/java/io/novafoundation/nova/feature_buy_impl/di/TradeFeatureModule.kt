package io.novafoundation.nova.feature_buy_impl.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.webView.InterceptingWebViewClientFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoBuyRequestInterceptorFactory
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoSellRequestInterceptorFactory
import io.novafoundation.nova.feature_buy_impl.BuildConfig
import io.novafoundation.nova.feature_buy_impl.presentation.trade.RealTradeTokenRegistry
import io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.banxa.BanxaProvider
import io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.mercurio.MercuryoIntegratorFactory
import io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.mercurio.MercuryoProvider
import io.novafoundation.nova.feature_buy_impl.presentation.trade.providers.transak.TransakProvider
import io.novafoundation.nova.feature_buy_impl.presentation.mixin.TradeMixinFactory
import io.novafoundation.nova.feature_buy_impl.presentation.trade.interceptors.mercuryo.RealMercuryoBuyRequestInterceptorFactory
import io.novafoundation.nova.feature_buy_impl.presentation.trade.interceptors.mercuryo.RealMercuryoSellRequestInterceptorFactory
import okhttp3.OkHttpClient

@Module
class BuyFeatureModule {

    @Provides
    @FeatureScope
    fun provideMercuryoSellRequestInterceptorFactory(
        gson: Gson,
        okHttpClient: OkHttpClient
    ): MercuryoSellRequestInterceptorFactory = RealMercuryoSellRequestInterceptorFactory(
        gson = gson,
        okHttpClient = okHttpClient
    )

    @Provides
    @FeatureScope
    fun provideMercuryoBuyRequestInterceptorFactory(
        gson: Gson,
        okHttpClient: OkHttpClient
    ): MercuryoBuyRequestInterceptorFactory = RealMercuryoBuyRequestInterceptorFactory(
        gson = gson,
        okHttpClient = okHttpClient
    )

    @Provides
    @FeatureScope
    fun provideMercuryoIntegratorFactory(
        mercuryoBuyInterceptorFactory: MercuryoBuyRequestInterceptorFactory,
        mercuryoSellInterceptorFactory: MercuryoSellRequestInterceptorFactory,
        interceptingWebViewClientFactory: InterceptingWebViewClientFactory,
    ): MercuryoIntegratorFactory {
        return MercuryoIntegratorFactory(
            mercuryoBuyInterceptorFactory,
            mercuryoSellInterceptorFactory,
            interceptingWebViewClientFactory
        )
    }

    @Provides
    @FeatureScope
    fun provideBanxaProvider(): BanxaProvider {
        return BanxaProvider(BuildConfig.BANXA_HOST)
    }

    @Provides
    @FeatureScope
    fun provideMercuryoProvider(integratorFactory: MercuryoIntegratorFactory): MercuryoProvider {
        return MercuryoProvider(
            host = BuildConfig.MERCURYO_HOST,
            widgetId = BuildConfig.MERCURYO_WIDGET_ID,
            secret = BuildConfig.MERCURYO_SECRET,
            integratorFactory = integratorFactory
        )
    }

    @Provides
    @FeatureScope
    fun provideTransakProvider(): TransakProvider {
        val environment = if (BuildConfig.DEBUG) "STAGING" else "PRODUCTION"

        return TransakProvider(
            host = BuildConfig.TRANSAK_HOST,
            apiKey = BuildConfig.TRANSAK_TOKEN,
            environment = environment
        )
    }

    @Provides
    @FeatureScope
    fun provideBuyTokenIntegration(
        transakProvider: TransakProvider,
        mercuryoProvider: MercuryoProvider,
        banxaProvider: BanxaProvider
    ): TradeTokenRegistry {
        return RealTradeTokenRegistry(
            providers = listOf(
                mercuryoProvider,
                transakProvider,
                banxaProvider,
            )
        )
    }

    @Provides
    @FeatureScope
    fun provideBuyMixinFactory(
        buyTokenRegistry: TradeTokenRegistry
    ): TradeMixin.Factory = TradeMixinFactory(
        buyTokenRegistry = buyTokenRegistry
    )
}
