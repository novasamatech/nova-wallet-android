package io.novafoundation.nova.feature_swap_core.di.conversions

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.RealHydraDxQuotingFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.OmniPoolQuotingSourceFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap.StableSwapQuotingSourceFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.xyk.XYKSwapQuotingSourceFactory
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class HydraDxConversionModule {

    @Provides
    @IntoSet
    fun provideOmniPoolSourceFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        chainRegistry: ChainRegistry,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    ): HydraDxQuotingSource.Factory<*> {
        return OmniPoolQuotingSourceFactory(
            remoteStorageSource = remoteStorageSource,
            chainRegistry = chainRegistry,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter
        )
    }

    @Provides
    @IntoSet
    fun provideStableSwapSourceFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
        gson: Gson,
    ): HydraDxQuotingSource.Factory<*> {
        return StableSwapQuotingSourceFactory(
            remoteStorageSource = remoteStorageSource,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            gson = gson,
        )
    }

    @Provides
    @IntoSet
    fun provideXykSwapSourceFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter
    ): HydraDxQuotingSource.Factory<*> {
        return XYKSwapQuotingSourceFactory(
            remoteStorageSource,
            hydraDxAssetIdConverter
        )
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
