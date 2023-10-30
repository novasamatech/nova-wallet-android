package io.novafoundation.nova.feature_buy_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_buy_api.domain.BuyTokenRegistry
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixin
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import io.novafoundation.nova.feature_buy_impl.BuildConfig
import io.novafoundation.nova.feature_buy_impl.domain.RealBuyTokenRegistry
import io.novafoundation.nova.feature_buy_impl.domain.providers.BanxaProvider
import io.novafoundation.nova.feature_buy_impl.domain.providers.MercuryoProvider
import io.novafoundation.nova.feature_buy_impl.domain.providers.TransakProvider
import io.novafoundation.nova.feature_buy_impl.presentation.mixin.BuyMixinFactory
import io.novafoundation.nova.feature_buy_impl.presentation.mixin.RealBuyMixinUi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class BuyFeatureModule {

    @Provides
    @FeatureScope
    fun provideBanxaProvider(): BanxaProvider {
        return BanxaProvider(BuildConfig.BANXA_HOST)
    }

    @Provides
    @FeatureScope
    fun provideMercuryoProvider(): MercuryoProvider {
        return MercuryoProvider(
            host = BuildConfig.MERCURYO_HOST,
            widgetId = BuildConfig.MERCURYO_WIDGET_ID,
            secret = BuildConfig.MERCURYO_SECRET
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
    ): BuyTokenRegistry {
        return RealBuyTokenRegistry(
            providers = listOf(
                transakProvider,
                banxaProvider,
                mercuryoProvider
            )
        )
    }

    @Provides
    @FeatureScope
    fun provideBuyMixinFactory(
        buyTokenRegistry: BuyTokenRegistry,
        chainRegistry: ChainRegistry,
        accountUseCase: SelectedAccountUseCase,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    ): BuyMixin.Factory = BuyMixinFactory(
        buyTokenRegistry = buyTokenRegistry,
        chainRegistry = chainRegistry,
        accountUseCase = accountUseCase,
        awaitableMixinFactory = actionAwaitableMixinFactory
    )

    @Provides
    @FeatureScope
    fun provideBuyMixinUi(): BuyMixinUi {
        return RealBuyMixinUi()
    }
}
