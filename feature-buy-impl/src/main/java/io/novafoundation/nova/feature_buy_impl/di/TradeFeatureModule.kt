package io.novafoundation.nova.feature_buy_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import io.novafoundation.nova.feature_buy_impl.BuildConfig
import io.novafoundation.nova.feature_buy_impl.domain.RealTradeTokenRegistry
import io.novafoundation.nova.feature_buy_impl.domain.providers.banxa.BanxaProvider
import io.novafoundation.nova.feature_buy_impl.domain.providers.mercurio.MercuryoIntegratorFactory
import io.novafoundation.nova.feature_buy_impl.domain.providers.mercurio.MercuryoProvider
import io.novafoundation.nova.feature_buy_impl.domain.providers.transak.TransakProvider
import io.novafoundation.nova.feature_buy_impl.presentation.mixin.TradeMixinFactory
import io.novafoundation.nova.feature_buy_impl.presentation.mixin.RealBuyMixinUi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class BuyFeatureModule {

    @Provides
    @FeatureScope
    fun provideMercuryoIntegratorFactory(): MercuryoIntegratorFactory {
        return MercuryoIntegratorFactory()
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

    @Provides
    @FeatureScope
    fun provideBuyMixinUi(): BuyMixinUi {
        return RealBuyMixinUi()
    }
}
