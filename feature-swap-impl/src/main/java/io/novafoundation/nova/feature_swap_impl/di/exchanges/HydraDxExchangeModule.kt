package io.novafoundation.nova.feature_swap_impl.di.exchanges

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.fee.types.hydra.HydrationFeeInjector
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuoting
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationPriceConversionFallback
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxExchangeFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.OmniPoolSwapSourceFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.HydraDxNovaReferral
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals.RealHydraDxNovaReferral
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.StableSwapSourceFactory
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.XYKSwapSourceFactory
import io.novafoundation.nova.feature_swap_impl.domain.AssetInAdditionalSwapDeductionUseCase
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
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
    fun provideOmniPoolSourceFactory(): HydraDxSwapSource.Factory<*> {
        return OmniPoolSwapSourceFactory()
    }

    @Provides
    @IntoSet
    fun provideStableSwapSourceFactory(
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    ): HydraDxSwapSource.Factory<*> {
        return StableSwapSourceFactory(hydraDxAssetIdConverter)
    }

    @Provides
    @IntoSet
    fun provideXykSwapSourceFactory(): HydraDxSwapSource.Factory<*> {
        return XYKSwapSourceFactory()
    }

    @Provides
    @FeatureScope
    fun provideHydraDxExchangeFactory(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
        hydraDxAssetIdConverter: HydraDxAssetIdConverter,
        hydraDxNovaReferral: HydraDxNovaReferral,
        swapSourceFactories: Set<@JvmSuppressWildcards HydraDxSwapSource.Factory<*>>,
        quotingFactory: HydraDxQuoting.Factory,
        hydrationFeeInjector: HydrationFeeInjector,
        chainStateRepository: ChainStateRepository,
        swapDeductionUseCase: AssetInAdditionalSwapDeductionUseCase,
        hydrationPriceConversionFallback: HydrationPriceConversionFallback,
    ): HydraDxExchangeFactory {
        return HydraDxExchangeFactory(
            remoteStorageSource = remoteStorageSource,
            sharedRequestsBuilderFactory = sharedRequestsBuilderFactory,
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            hydraDxNovaReferral = hydraDxNovaReferral,
            swapSourceFactories = swapSourceFactories,
            quotingFactory = quotingFactory,
            hydrationFeeInjector = hydrationFeeInjector,
            chainStateRepository = chainStateRepository,
            swapDeductionUseCase = swapDeductionUseCase,
            hydrationPriceConversionFallback = hydrationPriceConversionFallback
        )
    }
}
