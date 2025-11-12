package io.novafoundation.nova.feature_swap_core.di.conversions

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.RealHydraDxQuotingFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave.AaveSwapQuotingSourceFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.OmniPoolQuotingSourceFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.StableSwapQuotingSourceFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.XYKSwapQuotingSourceFactory
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource

@Module(includes = [HydraDxBindsModule::class])
class HydraDxConversionModule {

    @Provides
    @IntoSet
    fun provideOmniPoolSourceFactory(implementation: OmniPoolQuotingSourceFactory): HydraDxQuotingSource.Factory<*> {
        return implementation
    }

    @Provides
    @IntoSet
    fun provideStableSwapSourceFactory(implementation: StableSwapQuotingSourceFactory): HydraDxQuotingSource.Factory<*> {
        return implementation
    }

    @Provides
    @IntoSet
    fun provideXykSwapSourceFactory(implementation: XYKSwapQuotingSourceFactory): HydraDxQuotingSource.Factory<*> {
        return implementation
    }

    @Provides
    @IntoSet
    fun provideAavePoolQuotingSourceFactory(implementation: AaveSwapQuotingSourceFactory): HydraDxQuotingSource.Factory<*> {
        return implementation
    }

    @Provides
    @FeatureScope
    fun provideHydraDxAssetConversionFactory(
        conversionSourceFactories: Set<@JvmSuppressWildcards HydraDxQuotingSource.Factory<*>>,
    ): HydraDxQuoting.Factory {
        return RealHydraDxQuotingFactory(
            conversionSourceFactories = conversionSourceFactories,
        )
    }
}
