package io.novafoundation.nova.feature_swap_impl.di.exchanges

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraDxAssetConversionFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.HydraDxNovaReferral
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.RealHydraDxNovaReferral
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.OmniPoolSwapSourceFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.StableSwapSourceFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.XYKSwapSourceFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.RealHydraDxAssetConversionFactory
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class HydraDxExchangeModule {

    @Provides
    @FeatureScope
    fun provideHydraDxNovaReferral(): HydraDxNovaReferral {
        return RealHydraDxNovaReferral()
    }

    @Provides
    @IntoSet
    fun provideOmniPoolSourceFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        chainRegistry: ChainRegistry,
        assetSourceRegistry: AssetSourceRegistry,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    ): HydraDxSwapSource.Factory {
        return OmniPoolSwapSourceFactory(
            remoteStorageSource = remoteStorageSource,
            chainRegistry = chainRegistry,
            assetSourceRegistry = assetSourceRegistry,
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
    ): HydraDxSwapSource.Factory {
        return StableSwapSourceFactory(
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
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
        assetSourceRegistry: AssetSourceRegistry
    ): HydraDxSwapSource.Factory {
        return XYKSwapSourceFactory(
            remoteStorageSource = remoteStorageSource,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            assetSourceRegistry = assetSourceRegistry
        )
    }


    @Provides
    @FeatureScope
    fun provideHydraDxAssetConversionFactory(
        swapSourceFactories: Set<@JvmSuppressWildcards HydraDxSwapSource.Factory>,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    ): HydraDxAssetConversionFactory {
        return RealHydraDxAssetConversionFactory(
            swapSourceFactories,
            hydraDxAssetIdConverter
        )
    }

    @Provides
    @FeatureScope
    fun provideHydraDxExchangeFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
        extrinsicService: ExtrinsicService,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
        hydraDxNovaReferral: HydraDxNovaReferral,
        swapSourceFactories: Set<@JvmSuppressWildcards HydraDxSwapSource.Factory>,
        assetSourceRegistry: AssetSourceRegistry,
        hydraDxAssetConversionFactory: HydraDxAssetConversionFactory
    ): HydraDxExchangeFactory {
        return HydraDxExchangeFactory(
            remoteStorageSource = remoteStorageSource,
            sharedRequestsBuilderFactory = sharedRequestsBuilderFactory,
            extrinsicService = extrinsicService,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            hydraDxNovaReferral = hydraDxNovaReferral,
            swapSourceFactories = swapSourceFactories,
            assetSourceRegistry = assetSourceRegistry,
            assetConversionFactory = hydraDxAssetConversionFactory
        )
    }
}
