package io.novafoundation.nova.feature_swap_core.di.conversions

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.RealHydrationPriceConversionFallback
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationPriceConversionFallback

@Module
internal interface HydraDxBindsModule {

    @Binds
    fun bindHydrationPriceConversionFallback(real: RealHydrationPriceConversionFallback): HydrationPriceConversionFallback
}
