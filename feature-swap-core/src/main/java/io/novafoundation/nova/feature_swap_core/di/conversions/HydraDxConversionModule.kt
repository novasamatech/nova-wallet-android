package io.novafoundation.nova.feature_swap_core.di.conversions

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxAssetConversionFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxConversionSource
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.omnipool.OmniPoolConversionSourceFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.stableswap.StableConversionSourceFactory
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.impl.xyk.XYKConversionSourceFactory
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.RealHydraDxAssetConversionFactory
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
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
    ): HydraDxConversionSource.Factory {
        return OmniPoolConversionSourceFactory(
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
        chainStateRepository: ChainStateRepository
    ): HydraDxConversionSource.Factory {
        return StableConversionSourceFactory(
            remoteStorageSource = remoteStorageSource,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            gson = gson,
            chainStateRepository = chainStateRepository
        )
    }

    @Provides
    @IntoSet
    fun provideXykSwapSourceFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter
    ): HydraDxConversionSource.Factory {
        return XYKConversionSourceFactory(
            remoteStorageSource,
            hydraDxAssetIdConverter
        )
    }

    @Provides
    @FeatureScope
    fun provideHydraDxAssetConversionFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        conversionSourceFactories: Set<@JvmSuppressWildcards HydraDxConversionSource.Factory>,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    ): HydraDxAssetConversionFactory {
        return RealHydraDxAssetConversionFactory(
            remoteStorageSource = remoteStorageSource,
            conversionSourceFactories = conversionSourceFactories,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter
        )
    }
}
