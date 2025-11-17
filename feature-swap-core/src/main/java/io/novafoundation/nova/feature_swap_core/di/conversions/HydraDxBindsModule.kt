package io.novafoundation.nova.feature_swap_core.di.conversions

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.RealHydrationAcceptedFeeCurrenciesFetcher
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.RealHydrationPriceConversionFallback
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationAcceptedFeeCurrenciesFetcher
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationPriceConversionFallback

@Module
internal interface HydraDxBindsModule {

    @Binds
    fun bindHydrationPriceConversionFallback(real: RealHydrationPriceConversionFallback): HydrationPriceConversionFallback

    @Binds
    @FeatureScope
    fun bindHydrationAcceptedFeeCurrenciesFetcher(real: RealHydrationAcceptedFeeCurrenciesFetcher): HydrationAcceptedFeeCurrenciesFetcher
}
