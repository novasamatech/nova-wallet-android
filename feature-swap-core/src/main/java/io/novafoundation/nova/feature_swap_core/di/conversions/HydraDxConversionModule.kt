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
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

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
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        conversionSourceFactories: Set<@JvmSuppressWildcards HydraDxQuotingSource.Factory<*>>,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    ): HydraDxQuoting.Factory {
        return RealHydraDxQuotingFactory(
            remoteStorageSource = remoteStorageSource,
            conversionSourceFactories = conversionSourceFactories,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter
        )
    }
}
