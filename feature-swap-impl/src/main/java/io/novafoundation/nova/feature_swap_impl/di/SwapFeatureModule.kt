package io.novafoundation.nova.feature_swap_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.domain.swap.RealSwapService

@Module
class SwapFeatureModule {

    @Provides
    @FeatureScope
    fun provideAssetConversionExchangeFactory(): AssetConversionExchangeFactory {
        return AssetConversionExchangeFactory()
    }

    @FeatureScope
    @Provides
    fun provideSwapService(
        assetConversionExchangeFactory: AssetConversionExchangeFactory
    ): SwapService {
        return RealSwapService(assetConversionExchangeFactory)
    }
}
